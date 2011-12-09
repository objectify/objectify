package com.googlecode.objectify.impl.engine;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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
import com.googlecode.objectify.impl.SessionValue.Upgrade;
import com.googlecode.objectify.impl.cmd.ObjectifyImpl;
import com.googlecode.objectify.impl.translate.LoadContext;
import com.googlecode.objectify.util.ResultWrapper;

/**
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
		/** During each round we track the keys we will need to satisfy the request */
		Set<com.google.appengine.api.datastore.Key> pending = new HashSet<com.google.appengine.api.datastore.Key>();

		/** After execution, we'll have one of these */
		Result<Map<com.google.appengine.api.datastore.Key, Entity>> entities;
		
		/**
		 * We keep track of the ones we have already translated in this round so that we can have circular references.
		 * This gets initialized  
		 */
		Map<Key<?>, Object> translated;
		
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
					if (translated == null) {
						translated = new HashMap<Key<?>, Object>(entities.now().size() * 2);
						
						LoadContext ctx = new LoadContext(ofy, LoadEngine.this);
						
						for (Entity ent: entities.now().values()) {
							Key<?> key = Key.create(ent.getKey());
							Object entity = ofy.load(ent, ctx);
							translated.put(key, entity);
						}
						
						ctx.done();
					}
					
					return (T)translated.get(key);
				}

				@Override
				public String toString() {
					return "(Round Result of " + key + ")";
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
			entities = new ResultAdapter<Map<com.google.appengine.api.datastore.Key, Entity>>(fut);
		}
		
		/** */
		@Override
		public String toString() {
			return (translated == null ? "pending:" : "executed:") + pending.toString();
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
	public LoadEngine(ObjectifyImpl ofy, AsyncDatastoreService ads, Session session, Set<String> groups) {
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
		SessionValue<T> sent = this.ensureSessionContent(key);	// might add the parents too!
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
	private <T> SessionValue<T> ensureSessionContent(Key<T> key) {
		SessionValue<T> sv = session.get(key);
		if (sv == null) {
			
			Result<T> result = round.get(key);
			sv = new SessionValue<T>(key, result, groups);
			session.add(sv);
			
		} else if (!sv.isLoaded(groups)) {
			final SessionValue<T> svFinal = sv;
			sv.setResult(new ResultWrapper<T, T>(sv.getResult()) {
				@Override
				protected T wrap(T orig) {
					if (!svFinal.getUpgrades().isEmpty()) {
						
						// Prepare any that need it
						for (Upgrade<?> upgrade: svFinal.getUpgrades()) {
							if (upgrade.shouldLoad(groups)) {
								if (log.isLoggable(Level.FINEST))
									log.finest("Reload with groups " + groups + " upgrades: " + upgrade);

								upgrade.prepare(LoadEngine.this);
							}
						}
						
						// A no-op if nothing was prepared.
						execute();
						
						// Then execute + remove any that were prepared
						Iterator<Upgrade<?>> upgradesIt = svFinal.getUpgrades().iterator();
						while (upgradesIt.hasNext()) {
							Upgrade<?> upgrade = upgradesIt.next();
							if (upgrade.isPrepared()) {
								
								upgrade.doUpgrade();
								
								// Remove it from the list of upgrades that need to be filled
								upgradesIt.remove();
							}
						}
					}
					
					return orig;
				}
				
				@Override
				public String toString() {
					return "(upgrade wrapper for " + groups + ")";
				}
			});
			
			sv.addLoaded(groups);
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
		
		return sv;
	}
	
	/**
	 * @return true if the specified property should be loaded in this batch
	 */
	public boolean shouldLoad(Property property) {
		return property.shouldLoad(groups);
	}

	/**
	 * Register an upgrade on the specified root entity.  Assumes the root entity has already been
	 * added to the session, which should be a safe assumption since the entity gets loaded only
	 * after it is added to the session.
	 */
	public void registerUpgrade(Key<?> root, Upgrade<?> upgrade) {
		// This should always exist since we're translating something that was just put in the session
		SessionValue<?> sv = session.get(root);
		sv.getUpgrades().add(upgrade);
	}
	
	/**
	 * @return the currently enabled set of load groups
	 */
	public Set<String> getLoadGroups() {
		return this.groups;
	}
}