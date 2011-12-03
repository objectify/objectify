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
import com.googlecode.objectify.annotation.Load;
import com.googlecode.objectify.impl.EntityMetadata;
import com.googlecode.objectify.impl.ResultAdapter;
import com.googlecode.objectify.impl.Session;
import com.googlecode.objectify.impl.SessionEntity;
import com.googlecode.objectify.impl.TypeUtils;
import com.googlecode.objectify.impl.cmd.ObjectifyImpl;
import com.googlecode.objectify.impl.translate.LoadContext;
import com.googlecode.objectify.util.ResultWrapper;

/**
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class LoadBatch
{
	/** */
	private static final Logger log = Logger.getLogger(LoadBatch.class.getName());
	
	/** 
	 * Each round in the series of fetches required to complete a batch.  A round executes when
	 * the value is obtained (via now()) for a Result that was created as part of this round.
	 * When a round executes, a new round is created. 
	 */
	class Round {
		/** During each round we track the keys we will need to satisfy the request */
		Set<com.google.appengine.api.datastore.Key> pending = new HashSet<com.google.appengine.api.datastore.Key>();

		/** After execution, we'll have one of these */
		Result<Map<Key<?>, Object>> translated;

		/** 
		 * Adds a key to our pending queue and returns a result which will provide the translated value.
		 */
		public <T> Result<T> get(final Key<T> key) {
			if (log.isLoggable(Level.FINEST))
				log.finest("Adding to round: " + key);
			
			this.pending.add(key.getRaw());
			
			return new Result<T>() {
				@Override
				@SuppressWarnings("unchecked")
				public T now() {
					return (T)translated.now().get(key);
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
			
			Future<Map<com.google.appengine.api.datastore.Key, Entity>> fut = ads.get(ofy.getTxnRaw(), pending);
			Result<Map<com.google.appengine.api.datastore.Key, Entity>> adapted = new ResultAdapter<Map<com.google.appengine.api.datastore.Key, Entity>>(fut);
			translated = new ResultWrapper<Map<com.google.appengine.api.datastore.Key, Entity>, Map<Key<?>, Object>>(adapted) {
				@Override
				protected Map<Key<?>, Object> wrap(Map<com.google.appengine.api.datastore.Key, Entity> from) {
					// This is where the fun happens.  We create a LoadContext and translate the whole result.  That
					// process may add items to the pending queue.  As long as the queue has contents, we keep executing.
					
					Map<Key<?>, Object> result = new HashMap<Key<?>, Object>(from.size() * 2);
					LoadContext ctx = new LoadContext(ofy, LoadBatch.this);
					
					for (Map.Entry<com.google.appengine.api.datastore.Key, Entity> entry: from.entrySet()) {
						Key<?> key = Key.create(entry.getKey());
						Object entity = ofy.load(entry.getValue(), ctx);
						result.put(key, entity);
					}
					
					ctx.done();
					
					return result;
				}
			};
		}
	}
	
	/** */
	ObjectifyImpl ofy;
	AsyncDatastoreService ads;
	Session session;
	Set<String> groups;
	
	/** The current round, replaced whenever the round executes */
	Round round = new Round();
	
	/**
	 */
	public LoadBatch(ObjectifyImpl ofy, AsyncDatastoreService ads, Session session, Set<String> groups) {
		this.ofy = ofy;
		this.ads = ads;
		this.session = session;
		this.groups = groups;
	}
	
	/** Get the set of groups that are enabled on this batch */
	public Set<String> getGroups() { return this.groups; }
	
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
	 */
	public <T> Result<T> getResult(Key<T> key) {
		SessionEntity<T> sent = this.ensureSessionContent(key);	// might add the parents too!
		return sent.getResult();
	}
	
	/**
	 * Starts asychronous fetching of the batch.
	 */
	public void execute() {
		while (round.hasPending()) {
			Round old = round;
			round = new Round();
			old.execute();
		}
	}
	
	/**
	 * Makes sure that the session contains the right Result<?>s for the key and possibly
	 * its parent keys depending on the @Load commands.  This is a recursive method.
	 */
	private <T> SessionEntity<T> ensureSessionContent(Key<T> key) {
		SessionEntity<T> sent = session.get(key);
		if (sent == null) {
			Result<T> result = round.get(key);
			sent = new SessionEntity<T>(key, result);
			session.add(sent);
		}
		
		// Now check to see if we need to recurse and add our parent to the round
		if (key.getParent() != null) {
			EntityMetadata<?> meta = ofy.getFactory().getMetadata(key);
			if (meta != null) {
				if (meta.getKeyMetadata().shouldLoadParent(groups)) {
					ensureSessionContent(key.getParent());
				}
			}
		}
		
		return sent;
	}
	
	/**
	 * @param load can be null, which will always produce false
	 * @return true if the specified load annotation should be loaded in this batch
	 */
	public boolean shouldLoad(Load load) {
		return TypeUtils.shouldLoad(load, groups);
	}
}