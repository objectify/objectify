package com.googlecode.objectify.impl.cmd;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.Result;
import com.googlecode.objectify.cmd.Saver;
import com.googlecode.objectify.util.ResultWrapper;


/**
 * Implementation of the Put interface.
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
class SaverImpl implements Saver
{
	/** */
	ObjectifyImpl ofy;
	
	/** */
	SaverImpl(ObjectifyImpl ofy) {
		this.ofy = ofy;
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Put#entity(java.lang.Object)
	 */
	@Override
	public <K, E extends K> Result<Key<K>> entity(E entity) {
		Result<Map<Key<K>, E>> base = entities(Collections.singleton(entity));
		
		return new ResultWrapper<Map<Key<K>, E>, Key<K>>(base) {
			@Override
			protected Key<K> wrap(Map<Key<K>, E> base) {
				return base.keySet().iterator().next();
			}
		}; 
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Put#entities(E[])
	 */
	@Override
	public <K, E extends K> Result<Map<Key<K>, E>> entities(E... entities) {
		return entities(Arrays.asList(entities));
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Put#entities(java.lang.Iterable)
	 */
	@Override
	public <K, E extends K> Result<Map<Key<K>, E>> entities(final Iterable<? extends E> entities) {
		return ofy.createWriteEngine().save(entities);
	}

}
