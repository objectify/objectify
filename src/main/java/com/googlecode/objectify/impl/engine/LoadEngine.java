package com.googlecode.objectify.impl.engine;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.appengine.api.datastore.AsyncDatastoreService;
import com.google.appengine.api.datastore.Entity;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.Result;
import com.googlecode.objectify.impl.EntityMetadata;
import com.googlecode.objectify.impl.Property;
import com.googlecode.objectify.impl.ResultAdapter;
import com.googlecode.objectify.impl.Session;
import com.googlecode.objectify.impl.SessionValue;
import com.googlecode.objectify.impl.cmd.LoaderImpl;
import com.googlecode.objectify.impl.cmd.ObjectifyImpl;
import com.googlecode.objectify.impl.translate.LoadContext;
import com.googlecode.objectify.util.ResultNow;

/**
 * Represents one "batch" of loading.  Get a number of Result<?> objects, then execute().  Some work is done
 * right away, some work is done on the first get().  There might be multiple rounds of execution to process
 * all the @Load groups, but that is invisible outside this class.
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class LoadEngine
{
	/** */
	private static final Logger log = Logger.getLogger(LoadEngine.class.getName());
	
	/** 
	 * Each round in the series of fetches required to complete a batch.  A round executes when
	 * the value is obtained (via now()) for a Result that was created as part of this round.
	 * When a round executes, a new round is created. 
	 */
	class Round {
		/** When the round is complete (executed and lazily translated), this will hold all the data. */
		Map<Key<?>, Object> translated;
		
		/** Entities that have been enlisted in this round. Might come from session, might need to be fetched. */
		Map<Key<?>, Result<Entity>> enlisted = new HashMap<Key<?>, Result<Entity>>();
		
		/** The keys we will need to fetch; might not be any if everything came from the session */
		Set<com.google.appengine.api.datastore.Key> pending = new HashSet<com.google.appengine.api.datastore.Key>();

		/** If there were any pending, we will have fetched entities after execution */
		Result<Map<com.google.appengine.api.datastore.Key, Entity>> fetched;
		
		/** 
		 * Gets a result, using the session cache if possible.
		 */
		public <T> Result<T> get(final Key<T> key) {
			SessionValue sv = session.get(key);
			if (sv == null) {
				if (log.isLoggable(Level.FINEST))
					log.finest("Adding to round (session miss): " + key);
				
				this.pending.add(key.getRaw());
				
				sv = new SessionValue(key, new Result<Entity>() {
					@Override
					public Entity now() {
						return fetched.now().get(key.getRaw());
					}
					
					@Override
					public String toString() {
						return "(Fetch result for " + key + ")";
					}
				});
				session.add(sv);
			} else {
				if (log.isLoggable(Level.FINEST))
					log.finest("Adding to round (session hit): " + key);
			}
			
			enlisted.put(key, sv.getResult());
			
			return new Result<T>() {
				@Override
				@SuppressWarnings("unchecked")
				public T now() {
					if (translated == null) {
						if (log.isLoggable(Level.FINEST))
							log.finest("Translating " + enlisted.keySet());
						
						translated = new HashMap<Key<?>, Object>(enlisted.values().size() * 2);
						
						LoadContext ctx = new LoadContext(loader, LoadEngine.this);
						
						for (Result<Entity> rent: enlisted.values()) {
							Entity ent = rent.now();
							if (ent != null) {
								Key<?> key = Key.create(ent.getKey());
								Object entity = ofy.load(ent, ctx);
								translated.put(key, entity);
							}
						}
						
						ctx.done();
					}
					
					return (T)translated.get(key);
				}

				@Override
				public String toString() {
					return "(Round result of " + key + ")";
				}
			};
		}
		
		/** */
		public boolean hasPending() {
			return !pending.isEmpty();
		}
		
		/** Turn this into a result set */
		public void execute() {
			if (log.isLoggable(Level.FINEST))
				log.finest("Executing round: " + pending);
			
			if (!pending.isEmpty()) {
				Future<Map<com.google.appengine.api.datastore.Key, Entity>> fut = ads.get(ofy.getTxnRaw(), pending);
				fetched = new ResultAdapter<Map<com.google.appengine.api.datastore.Key, Entity>>(fut);
			}
		}
		
		/** */
		@Override
		public String toString() {
			return (translated == null ? "pending:" : "executed:") + pending.toString();
		}
	}
	
	/** */
	LoaderImpl loader;
	ObjectifyImpl ofy;
	AsyncDatastoreService ads;
	Session session;
	
	/** We recycle instances during each batch, across rounds */
	Map<Key<?>, Result<?>> instanceCache = new HashMap<Key<?>, Result<?>>();
	
	/** The current round, replaced whenever the round executes */
	Round round = new Round();
	
	/**
	 */
	public LoadEngine(LoaderImpl loader) {
		this.loader = loader;
		this.ofy = loader.getObjectifyImpl();
		this.session = loader.getObjectifyImpl().getSession();
		this.ads = loader.getObjectifyImpl().createAsyncDatastoreService();
		
		if (log.isLoggable(Level.FINEST))
			log.finest("Starting load engine with groups " + loader.getLoadGroups());
	}
	
	/**
	 * The fundamental ref() operation.
	 */
	@SuppressWarnings("unchecked")
	public void loadRef(Ref<?> ref) {
		Result<Object> result = (Result<Object>)this.getResult(ref.key());
		((Ref<Object>)ref).set(result);
	}
	
	/**
	 * Convenience method that creates a new ref and loads it
	 */
	public <T> Ref<T> getRef(Key<T> key) {
		Ref<T> ref = Ref.create(key);
		loadRef(ref);
		return ref;
	}
	
	/**
	 * Gets the result, possibly from the session, putting it in the session if necessary.
	 * Also will recursively populate the instanceCache with @Load parents as appropriate.
	 */
	public <T> Result<T> getResult(Key<T> key) {
		@SuppressWarnings("unchecked")
		Result<T> result = (Result<T>)instanceCache.get(key);
		if (result == null) {
			result = round.get(key);
			instanceCache.put(key, result);
		}
		
		// Now check to see if we need to recurse and add our parent to the round
		if (key.getParent() != null) {
			EntityMetadata<?> meta = ofy.getFactory().getMetadata(key);
			if (meta != null) {
				if (meta.getKeyMetadata().shouldLoadParent(loader.getLoadGroups())) {
					getResult(key.getParent());
				}
			}
		}

		return result;
	}
	
	/**
	 * Starts asychronous fetching of the batch.
	 */
	public void execute() {
		Round old = round;
		round = new Round();
		old.execute();
	}
	
	/**
	 * @return true if the specified property should be loaded in this batch
	 */
	public boolean shouldLoad(Property property) {
		return property.shouldLoad(loader.getLoadGroups());
	}

	/**
	 * Stuffs an Entity into the session.  Called by non-hybrid queries to add results and eliminate batch fetching.
	 */
	public void stuffSession(Entity ent) {
		session.add(new SessionValue(Key.create(ent.getKey()), new ResultNow<Entity>(ent)));
	}
}