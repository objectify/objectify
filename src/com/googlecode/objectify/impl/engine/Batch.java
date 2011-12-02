package com.googlecode.objectify.impl.engine;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import com.google.appengine.api.datastore.AsyncDatastoreService;
import com.google.appengine.api.datastore.Entity;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.Result;
import com.googlecode.objectify.impl.EntityMetadata;
import com.googlecode.objectify.impl.ResultAdapter;
import com.googlecode.objectify.impl.Session;
import com.googlecode.objectify.impl.SessionEntity;
import com.googlecode.objectify.impl.cmd.ObjectifyImpl;
import com.googlecode.objectify.impl.translate.LoadContext;
import com.googlecode.objectify.util.ResultWrapper;

/**
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class Batch
{
	/** */
	@SuppressWarnings("unused")
	private static final Logger log = Logger.getLogger(Batch.class.getName());
	
	/** 
	 * Each round in the series of fetches required to complete a batch.  A round executes when
	 * the value is obtained (via now()) for a Result that was created as part of this round.
	 * When a round executes, a new round is created. 
	 */
	class Round {
		/** During each round we track the keys we will need to satisfy the request */
		Set<com.google.appengine.api.datastore.Key> pending = new HashSet<com.google.appengine.api.datastore.Key>();
		
		/** After execution, we'll have one of these */
		Result<Map<com.google.appengine.api.datastore.Key, Entity>> adapted;

		/** 
		 * Adds a key to our pending queue and returns a result which will provide the Entity value.  The
		 * first request for the value will trigger execution of the round. 
		 */
		public Result<Entity> get(final com.google.appengine.api.datastore.Key key) {
			this.pending.add(key);
			
			return new Result<Entity>() {
				@Override
				public Entity now() {
					Batch.this.execute();
					return adapted.now().get(key);
				}
			};
		}
		
		/** Turn this into a result set */
		private void execute() {
			if (adapted == null && !pending.isEmpty()) {
				Future<Map<com.google.appengine.api.datastore.Key, Entity>> fut = ads.get(ofy.getTxnRaw(), pending);
				adapted = new ResultAdapter<Map<com.google.appengine.api.datastore.Key, Entity>>(fut);
				
				// Start a new round, this one is finished
				round = new Round();
			}
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
	public Batch(ObjectifyImpl ofy, AsyncDatastoreService ads, Session session, Set<String> groups) {
		this.ofy = ofy;
		this.ads = ads;
		this.session = session;
		this.groups = groups;
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
	 */
	public <T> Result<T> getResult(Key<T> key) {
		this.ensureSessionContent(key);	// might add the parents too!
		return session.get(key).getResult();
	}
	
	/**
	 * Starts asychronous fetching of the batch.
	 */
	public void execute() {
		round.execute();
	}
	
	/**
	 * Makes sure that the session contains the right Result<?>s for the key and possibly
	 * its parent keys depending on the @Load commands.  This is a recursive method.
	 */
	private void ensureSessionContent(Key<?> key) {
		SessionEntity sent = session.get(key);
		if (sent == null) {
			Result<?> result = createResult(key);
			sent = new SessionEntity(result);
			session.put(key, sent);
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
	}
	
	/**
	 * Create a result suitable for putting in the session
	 */
	private <T> Result<T> createResult(final Key<T> key) {
		return new ResultWrapper<Entity, T>(round.get(key.getRaw())) {
			@Override
			protected T wrap(Entity orig) {
				return ofy.load(orig, new LoadContext(ofy));
			}
		};
	}
}