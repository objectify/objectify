package com.googlecode.objectify.impl;

import com.google.cloud.datastore.FullEntity;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Result;
import com.googlecode.objectify.cmd.Saver;
import com.googlecode.objectify.impl.translate.SaveContext;
import com.googlecode.objectify.util.ResultWrapper;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;


/**
 * Implementation of the Put interface.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class SaverImpl implements Saver
{
	/** */
	private final ObjectifyImpl ofy;

	/** */
	public SaverImpl(final ObjectifyImpl ofy) {
		this.ofy = ofy;
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Saver#entity(java.lang.Object)
	 */
	@Override
	public <E> Result<Key<E>> entity(final E entity) {
		final Result<Map<Key<E>, E>> base = this.<E>entities(Collections.singleton(entity));

		return new ResultWrapper<Map<Key<E>, E>, Key<E>>(base) {
			private static final long serialVersionUID = 1L;

			@Override
			protected Key<E> wrap(final Map<Key<E>, E> base) {
				return base.keySet().iterator().next();
			}
		};
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Saver#entities(E[])
	 */
	@Override
	public <E> Result<Map<Key<E>, E>> entities(final E... entities) {
		return this.entities(Arrays.asList(entities));
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Saver#entities(java.lang.Iterable)
	 */
	@Override
	public <E> Result<Map<Key<E>, E>> entities(final Iterable<E> entities) {
		return ofy.createWriteEngine().save(entities);
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Saver#toEntity(java.lang.Object)
	 */
	@Override
	public FullEntity<?> toEntity(final Object pojo) {
		if (pojo instanceof FullEntity<?>) {
			return (FullEntity<?>)pojo;
		} else {
			@SuppressWarnings("unchecked")
			final EntityMetadata<Object> meta = (EntityMetadata<Object>)ofy.factory().getMetadata(pojo.getClass());
			return meta.save(pojo, new SaveContext());
		}
	}

}
