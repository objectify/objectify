package com.googlecode.objectify.impl;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.Result;
import com.googlecode.objectify.cmd.Inserter;
import com.googlecode.objectify.util.ResultWrapper;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;


/**
 * Implementation of the Inserter interface.
 */
public class InserterImpl implements Inserter {

	private final ObjectifyImpl ofy;

	public InserterImpl(final ObjectifyImpl ofy) {
		this.ofy = ofy;
	}

	@Override
	public <E> Result<Key<E>> entity(final E entity) {
		final Result<Map<Key<E>, E>> base = this.entities(Collections.singleton(entity));

		return new ResultWrapper<>(base) {
            private static final long serialVersionUID = 1L;

            @Override
            protected Key<E> wrap(final Map<Key<E>, E> base) {
                return base.keySet().iterator().next();
            }
        };
	}

	@Override
	public <E> Result<Map<Key<E>, E>> entities(final E... entities) {
		return this.entities(Arrays.asList(entities));
	}

	@Override
	public <E> Result<Map<Key<E>, E>> entities(final Iterable<E> entities) {
		return ofy.factory().span("insert", spanipulator -> {
			final Result<Map<Key<E>, E>> result = ofy.createWriteEngine().insert(entities);
			result.now();
			return result;
		});
	}
}
