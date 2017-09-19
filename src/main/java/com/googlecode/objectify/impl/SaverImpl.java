package com.googlecode.objectify.impl;

import com.google.appengine.api.datastore.Entity;
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
	private ObjectifyImpl<?> ofy;
	private Transactor<?> transactor;

	/** */
	public SaverImpl(ObjectifyImpl<?> ofy) {
		this.ofy = ofy;
		this.transactor = ofy.transactor();
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Saver#entity(java.lang.Object)
	 */
	@Override
	public <E> Result<Key<E>> entity(E entity) {
		Result<Map<Key<E>, E>> base = this.<E>entities(Collections.singleton(entity));

		return new ResultWrapper<Map<Key<E>, E>, Key<E>>(base) {
			private static final long serialVersionUID = 1L;

			@Override
			protected Key<E> wrap(Map<Key<E>, E> base) {
				return base.keySet().iterator().next();
			}
		};
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Saver#entities(E[])
	 */
	@Override
	public <E> Result<Map<Key<E>, E>> entities(E... entities) {
		return this.<E>entities(Arrays.asList(entities));
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Saver#entities(java.lang.Iterable)
	 */
	@Override
	public <E> Result<Map<Key<E>, E>> entities(final Iterable<E> entities) {
		return ObjectifyImpl.createWriteEngine(ofy, transactor).<E>save(entities);
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Saver#toEntity(java.lang.Object)
	 */
	@Override
	public Entity toEntity(Object pojo) {
		if (pojo instanceof Entity) {
			return (Entity)pojo;
		} else {
			@SuppressWarnings("unchecked")
			EntityMetadata<Object> meta = (EntityMetadata<Object>)ofy.factory().getMetadata(pojo.getClass());
			return meta.save(pojo, new SaveContext());
		}
	}

}
