package com.googlecode.objectify.util.cmd;

import java.util.Map;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.Result;
import com.googlecode.objectify.cmd.Saver;

public class SaverWrapper implements Saver {

	/** */
	private Saver base;

	/** */
	public SaverWrapper(Saver saver) {
		this.base = saver;
	}

	@Override
	public <K, E extends K> Result<Key<K>> entity(E entity) {
		return this.base.entity(entity);
	}

	@Override
	public <K, E extends K> Result<Map<Key<K>, E>> entities(Iterable<E> entities) {
		return this.base.entities(entities);
	}

	@Override
	public <K, E extends K> Result<Map<Key<K>, E>> entities(E... entities) {
		return this.base.entities(entities);
	}
}
