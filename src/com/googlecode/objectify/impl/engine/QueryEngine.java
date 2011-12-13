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
	 * @param txn can be null to not use transactions. 
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
		
		PreparedQuery pq = ads.prepare(loader.getObjectifyImpl().getTxnRaw(), query);
		final QueryResultIterable<Entity> source = pq.asQueryResultIterable(fetchOpts);
		
		return new QueryResultIterable<Ref<T>>() {
			@Override
			public QueryResultIterator<Ref<T>> iterator() {
				return new ChunkingToRefIterator<T>(source.iterator(), fetchOpts.getStartCursor(), fetchOpts.getChunkSize(), keysOnly, hybridize);
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

	
	/** Used in the hybrid iterator */
	private static class RefAndCursor<T> {
		public Ref<T> ref;
		public Cursor cursor;
		
		public RefAndCursor(Ref<T> ref, Cursor cursor) {
			this.ref = ref;
			this.cursor = cursor;
		}
	}
	
	/**
	 * Takes a keys-only iterable source, breaks it down into batches of a specific chunk size, and
	 * uses batch loading to load the actual values.  This makes @Cache and @Load annotations work.
	 */
	protected class ChunkingToRefIterator<T> implements QueryResultIterator<Ref<T>> {
		
		/** Input values */
		QueryResultIterator<Entity> source;
		int chunkSize;
		boolean keysOnly;
		boolean hybrid;
		
		/** As we process */
		Iterator<RefAndCursor<T>> batchIt;
		Cursor currentCursor;
		
		/** */
		public ChunkingToRefIterator(QueryResultIterator<Entity> source, Cursor startAt, int chunkSize, boolean keysOnly, boolean hybrid) {
			this.source = source;
			this.chunkSize = chunkSize;
			this.keysOnly = keysOnly;
			this.hybrid = hybrid;
			this.currentCursor = startAt;
			
			this.advanceBatch();
		}
		
		@Override
		public boolean hasNext() {
			return batchIt.hasNext();
		}

		@Override
		public Ref<T> next() {
			RefAndCursor<T> rac = batchIt.next();
			currentCursor = rac.cursor;
			
			if (!batchIt.hasNext())
				this.advanceBatch();
			
			return rac.ref;
		}
		
		private void advanceBatch() {
			LoadEngine loadEngine = loader.createLoadEngine();
			List<RefAndCursor<T>> racs = new ArrayList<RefAndCursor<T>>();

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
				
				racs.add(new RefAndCursor<T>(ref, source.getCursor()));
			}
			
			loadEngine.execute();
			batchIt = racs.iterator();
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}

		@Override
		public Cursor getCursor() {
			return currentCursor;
		}
	}
}