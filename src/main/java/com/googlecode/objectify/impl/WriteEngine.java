package com.googlecode.objectify.impl;

import com.google.cloud.datastore.FullEntity;
import com.google.common.collect.Lists;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.NamespaceManager;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.Result;
import com.googlecode.objectify.impl.translate.SaveContext;
import com.googlecode.objectify.util.Closeable;
import com.googlecode.objectify.util.ResultWrapper;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

/**
 * This is the master logic for saving and deleting entities from the datastore.  It provides the
 * fundamental operations that enable the rest of the API.  One of these engines is created for every operation;
 * upon completion, it is thrown away.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
@Slf4j
public class WriteEngine
{
	/** */
	protected final ObjectifyImpl ofy;

	/** */
	protected final AsyncDatastoreReaderWriter datastore;

	/** */
	protected final Session session;

	/** */
	protected final Deferrer deferrer;

	/**
	 */
	public WriteEngine(ObjectifyImpl ofy, AsyncDatastoreReaderWriter datastore, Session session, Deferrer deferrer) {
		this.ofy = ofy;
		this.datastore = datastore;
		this.session = session;
		this.deferrer = deferrer;
	}

	/**
	 * The fundamental put() operation.
	 */
	public <E> Result<Map<Key<E>, E>> save(Iterable<? extends E> entities) {
		log.trace("Saving {}", entities);

		// A hacky way of doing this but otherwise we have to adjust the save() contracts to take a namespace
		final Closeable unsetNamespace = ofy.getOptions().getNamespace() == null ? null : NamespaceManager.set(ofy.getOptions().getNamespace());
		try {
			final SaveContext ctx = new SaveContext();

			final List<FullEntity<?>> entityList = new ArrayList<>();
			for (final E obj : entities) {
				if (obj == null)
					throw new NullPointerException("Attempted to save a null entity");

				deferrer.undefer(ofy.getOptions(), obj);

				if (obj instanceof FullEntity) {
					entityList.add((FullEntity<?>)obj);
				} else {
					final EntityMetadata<E> metadata = factory().getMetadataForEntity(obj);
					final FullEntity<?> translated = metadata.save(obj, ctx);
					entityList.add(translated);
				}
			}

			// Need to make a copy of the original list because someone might clear it while we are async
			final List<? extends E> original = Lists.newArrayList(entities);

			// The CachingDatastoreService needs its own raw transaction
			final Future<List<com.google.cloud.datastore.Key>> raw = datastore.put(entityList);
			final Result<List<com.google.cloud.datastore.Key>> adapted = new ResultAdapter<>(raw);

			final Result<Map<Key<E>, E>> result = new ResultWrapper<List<com.google.cloud.datastore.Key>, Map<Key<E>, E>>(adapted) {
				private static final long serialVersionUID = 1L;

				@Override
				protected Map<Key<E>, E> wrap(List<com.google.cloud.datastore.Key> base) {
					Map<Key<E>, E> result = new LinkedHashMap<>(base.size() * 2);

					// One pass through the translated pojos to patch up any generated ids in the original objects
					// Iterator order should be exactly the same for keys and values
					Iterator<com.google.cloud.datastore.Key> keysIt = base.iterator();
					for (E obj : original) {
						com.google.cloud.datastore.Key k = keysIt.next();
						if (!(obj instanceof FullEntity<?>)) {
							KeyMetadata<E> metadata = factory().keys().getMetadataSafe(obj);
							if (metadata.isIdGeneratable())
								metadata.setLongId(obj, k.getId());
						}

						Key<E> key = Key.create(k);
						result.put(key, obj);

						// Also stuff this in the session
						session.addValue(key, obj);
					}

					log.trace("Saved {}", base);

					return result;
				}
			};

			if (ofy.getTransaction() != null)
				((PrivateAsyncTransaction)ofy.getTransaction()).enlist(result);

			return result;
		} finally {
			if (unsetNamespace != null)
				unsetNamespace.close();
		}
	}

	private ObjectifyFactory factory() {
		return ofy.factory();
	}

	/**
	 * The fundamental delete() operation.
	 */
	public Result<Void> delete(final Iterable<com.google.cloud.datastore.Key> keys) {
		for (com.google.cloud.datastore.Key key: keys)
			deferrer.undefer(ofy.getOptions(), Key.create(key));

		final Future<Void> fut = datastore.delete(keys);
		final Result<Void> adapted = new ResultAdapter<>(fut);
		final Result<Void> result = new ResultWrapper<Void, Void>(adapted) {
			@Override
			protected Void wrap(final Void orig) {
				for (com.google.cloud.datastore.Key key: keys)
					session.addValue(Key.create(key), null);

				return orig;
			}
		};

		if (ofy.getTransaction() != null)
			((PrivateAsyncTransaction)ofy.getTransaction()).enlist(result);

		return result;
	}
}