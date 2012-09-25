package com.googlecode.objectify.util.cmd;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.Result;
import com.googlecode.objectify.cmd.DeleteType;
import com.googlecode.objectify.cmd.Deleter;

public class DeleterWrapper implements Deleter {

	private Deleter base;

	public DeleterWrapper(Deleter deleter) {
		this.base = deleter;
	}

	@Override
	public DeleteType type(Class<?> type) {
		return this.base.type(type);
	}

	@Override
	public Result<Void> key(Key<?> key) {
		return this.base.key(key);
	}

	@Override
	public Result<Void> keys(Iterable<? extends Key<?>> keys) {
		return this.base.keys(keys);
	}

	@Override
	public Result<Void> keys(Key<?>... keys) {
		return this.base.keys(keys);
	}

	@Override
	public Result<Void> entity(Object entity) {
		return this.base.entity(entity);
	}

	@Override
	public Result<Void> entities(Iterable<?> entities) {
		return this.base.entities(entities);
	}

	@Override
	public Result<Void> entities(Object... entities) {
		return this.base.entities(entities);
	}
}
