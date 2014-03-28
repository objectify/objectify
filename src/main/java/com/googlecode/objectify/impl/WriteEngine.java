package com.googlecode.objectify.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.appengine.api.datastore.AsyncDatastoreService;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Transaction;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Result;
import com.googlecode.objectify.impl.translate.SaveContext;
import com.googlecode.objectify.util.ResultNow;
import com.googlecode.objectify.util.ResultWrapper;

/**
 * This is the master logic for saving and deleting entities from the datastore.  It provides the
 * fundamental operations that enable the rest of the API.  One of these engines is created for every operation;
 * upon completion, it is thrown away.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class WriteEngine
{
	/** */
	private static final Logger log = Logger.getLogger(WriteEngine.class.getName());

	/** */
	protected ObjectifyImpl<?> ofy;

	/** */
	protected AsyncDatastoreService ads;

	/** */
	protected Session session;

	/**
	 */
	public WriteEngine(ObjectifyImpl<?> ofy, AsyncDatastoreService ads, Session session) {
		this.ofy = ofy;
		this.ads = ads;
		this.session = session;
	}

	/** @return the transaction, or null if not */
	private Transaction getTransactionRaw() {
		return (ofy.getTransaction() == null) ? null : ofy.getTransaction().getRaw();
	}

	/**
	 * The fundamental put() operation.
	 */
	public <E> Result<Map<Key<E>, E>> save(final Iterable<? extends E> entities) {

		if (log.isLoggable(Level.FINEST))
			log.finest("Saving " + entities);

		final SaveContext ctx = new SaveContext(ofy);

		final List<Entity> entityList = new ArrayList<Entity>();
		for (E obj: entities) {
			if (obj == null)
				throw new NullPointerException("Attempted to save a null entity");

			if (obj instanceof Entity) {
				entityList.add((Entity)obj);
			} else {
				EntityMetadata<E> metadata = ofy.factory().getMetadataForEntity(obj);
				entityList.add(metadata.save(obj, ctx));
			}
		}

		// The CachingDatastoreService needs its own raw transaction
		Future<List<com.google.appengine.api.datastore.Key>> raw = ads.put(getTransactionRaw(), entityList);
		Result<List<com.google.appengine.api.datastore.Key>> adapted = new ResultAdapter<List<com.google.appengine.api.datastore.Key>>(raw);

		Result<Map<Key<E>, E>> result = new ResultWrapper<List<com.google.appengine.api.datastore.Key>, Map<Key<E>, E>>(adapted) {
			private static final long serialVersionUID = 1L;

			@Override
			protected Map<Key<E>, E> wrap(List<com.google.appengine.api.datastore.Key> base) {
				Map<Key<E>, E> result = new LinkedHashMap<Key<E>, E>(base.size() * 2);

				// One pass through the translated pojos to patch up any generated ids in the original objects
				// Iterator order should be exactly the same for keys and values
				Iterator<com.google.appengine.api.datastore.Key> keysIt = base.iterator();
				for (E obj: entities)
				{
					com.google.appengine.api.datastore.Key k = keysIt.next();
					if (!(obj instanceof Entity)) {
						KeyMetadata<E> metadata = Keys.getMetadataSafe(obj);
						if (metadata.isIdGeneratable())
							metadata.setLongId(obj, k.getId());
					}

					Key<E> key = Key.create(k);
					result.put(key, obj);

					// Also stuff this in the session
					session.add(key, new SessionValue<Object>(new ResultNow<Object>(obj), ctx.getReferences(obj)));
				}

				if (log.isLoggable(Level.FINEST))
					log.finest("Saved " + base);

				return result;
			}
		};

		if (ofy.getTransaction() != null)
			ofy.getTransaction().enlist(result);

		return result;
	}

	/**
	 * The fundamental delete() operation.
	 */
	public Result<Void> delete(final Iterable<com.google.appengine.api.datastore.Key> keys) {
		Future<Void> fut = ads.delete(getTransactionRaw(), keys);
		Result<Void> adapted = new ResultAdapter<Void>(fut);
		Result<Void> result = new ResultWrapper<Void, Void>(adapted) {
			private static final long serialVersionUID = 1L;

			@Override
			protected Void wrap(Void orig) {
				for (com.google.appengine.api.datastore.Key key: keys)
					session.add(Key.create(key), new SessionValue<Object>(new ResultNow<Object>(null)));

				return orig;
			}
		};

		if (ofy.getTransaction() != null)
			ofy.getTransaction().enlist(result);

		return result;
	}
}