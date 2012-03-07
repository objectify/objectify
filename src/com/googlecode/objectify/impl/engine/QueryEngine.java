package com.googlecode.objectify.impl.engine;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.appengine.api.datastore.AsyncDatastoreService;
import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Index;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.QueryResultIterable;
import com.google.appengine.api.datastore.QueryResultIterator;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.impl.cmd.LoaderImpl;
import com.googlecode.objectify.util.DatastoreUtils;

/**
 * Logic for dealing with queries.
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class QueryEngine
{
	/** */
	private static final Logger log = Logger.getLogger(QueryEngine.class.getName());
	
	/** */
	protected LoaderImpl loader;
	
	/**
	 */
	public QueryEngine(LoaderImpl loader) {
		this.loader = loader;
	}

	/**
	 * The fundamental query() operation, which provides Refs.  Might be a keys only query.
	 * @param hybridize if true, convert to a keys-only query + batch load
	 */
	public <T> QueryResultIterable<Ref<T>> query(com.google.appengine.api.datastore.Query query, final FetchOptions fetchOpts, final boolean hybridize) {
		// We might hybridize the query, so we need to know if the user really wanted a keysonly to begin with
		final boolean keysOnly = query.isKeysOnly();
		
		if (hybridize)
			query = DatastoreUtils.cloneQuery(query).setKeysOnly();
		
		if (log.isLoggable(Level.FINEST)) {
			String description = hybridize ? "hybrid" : keysOnly ? "keys-only" : "normal";
			log.finest("Starting " + description + " query");
		}
		
		AsyncDatastoreService ads = loader.getObjectifyImpl().createAsyncDatastoreService();
		
		final PreparedQuery pq = ads.prepare(loader.getObjectifyImpl().getTxnRaw(), query);
		
		return new QueryResultIterable<Ref<T>>() {
			@Override
			public QueryResultIterator<Ref<T>> iterator() {
				return new ChunkingToRefIterator<T>(pq, fetchOpts, keysOnly, hybridize);
			}
		};
	}

	/**
	 * The fundamental query count operation.  This is sufficiently different from normal query().
	 */
	public int queryCount(com.google.appengine.api.datastore.Query query, FetchOptions fetchOpts) {
		AsyncDatastoreService ads = loader.getObjectifyImpl().createAsyncDatastoreService();
		
		PreparedQuery pq = ads.prepare(loader.getObjectifyImpl().getTxnRaw(), query);
		return pq.countEntities(fetchOpts);
	}

	/**
	 * Takes a keys-only iterable source, breaks it down into batches of a specific chunk size, and
	 * uses batch loading to load the actual values.  This makes @Cache and @Load annotations work.
	 */
	protected class ChunkingToRefIterator<T> implements QueryResultIterator<Ref<T>> {
		
		/** Input values */
		PreparedQuery pq;
		QueryResultIterator<Entity> source;
		int chunkSize;
		boolean keysOnly;
		boolean hybrid;
		
		/** As we process */
		Iterator<Ref<T>> batchIt;
		Cursor baseCursor;
		int offsetIntoBatch;
		
		/** */
		public ChunkingToRefIterator(PreparedQuery pq, FetchOptions fetchOpts, boolean keysOnly, boolean hybrid) {
			this.pq = pq;
			this.source = pq.asQueryResultIterator(fetchOpts);
			this.chunkSize = fetchOpts.getChunkSize();
			this.keysOnly = keysOnly;
			this.hybrid = hybrid;
			
			this.advanceBatch();
		}
		
		@Override
		public boolean hasNext() {
			return batchIt.hasNext();
		}

		@Override
		public Ref<T> next() {
			Ref<T> ref = batchIt.next();
			offsetIntoBatch++;
			
			if (!batchIt.hasNext())
				this.advanceBatch();
			
			return ref;
		}
		
		private void advanceBatch() {
			LoadEngine loadEngine = loader.createLoadEngine();
			List<Ref<T>> refs = new ArrayList<Ref<T>>();
			
			// Initialize the cursor and the offset so that we can generate a cursor later
			baseCursor = source.getCursor();
			offsetIntoBatch = 0;

			for (int i=0; i<chunkSize; i++) {
				if (!source.hasNext())
					break;
				
				Entity ent = source.next();
				Key<T> key = Key.create(ent.getKey());
				Ref<T> ref = Ref.create(key);
				
				if (log.isLoggable(Level.FINEST))
					log.finest("Query found " + ent.getKey());
				
				if (!hybrid && !keysOnly)
					loadEngine.stuffSession(ent);
				
				if (!keysOnly)
					loadEngine.loadRef(ref);
				
				refs.add(ref);
			}
			
			loadEngine.execute();
			batchIt = refs.iterator();
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}

		/**
		 * From Alfred Fuller (principal GAE datastore guru):
		 * 
		 * Calling getCursor() for results in the middle of a batch forces the sdk to run a new query as seen here:
		 * http://code.google.com/p/googleappengine/source/browse/trunk/java/src/main/com/google/appengine/api/datastore/Cursor.java#70
		 * 
		 * Doing this for every result will definitely give you really bad performance. I have several yet to be implemented ideas
		 * that would solve this problem (which you potentially could push me into prioritizing), but I believe you can solve the
		 * performance problem today by saving the start_cursor an offset into the batch. Then you can evaluate the real cursor on
		 * demand using "query.asQueryResultIterator(withStartCursor(cursor).offset(n).limit(0)).getCursor()"
		 */
		@Override
		public Cursor getCursor() {
			if (offsetIntoBatch == 0)
				return source.getCursor();
			else
				return pq.asQueryResultIterator(FetchOptions.Builder.withStartCursor(baseCursor).offset(offsetIntoBatch).limit(0)).getCursor();
		}

		@Override
		public List<Index> getIndexList() {
			return this.source.getIndexList();
		}
	}
}