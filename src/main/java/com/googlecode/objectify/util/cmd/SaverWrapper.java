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
	public <E> Result<Key<E>> entity(E entity) {
		return this.base.entity(entity);
	}

	@Override
	public <E> Result<Map<Key<E>, E>> entities(Iterable<E> entities) {
		return this.base.entities(entities);
	}

	@Override
	public <E> Result<Map<Key<E>, E>> entities(E... entities) {
		return this.base.entities(entities);
	}
}
