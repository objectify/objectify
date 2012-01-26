package com.googlecode.objectify.util.cmd;

import java.util.Map;
import java.util.Set;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.cmd.LoadType;
import com.googlecode.objectify.cmd.Loader;

/**
 * Simple wrapper/decorator for a Loader.  Use it like this:
 * {@code class MyLoader extends LoaderWrapper<MyLoader>} 
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class LoaderWrapper<H extends LoaderWrapper<H>> extends SimpleQueryWrapper<H, Object> implements Loader
{
	/** */
	private Loader base;
	
	/** */
	public LoaderWrapper(Loader base) {
		super(base);
		this.base = base;
		this.base.setWrapper(this);
	}

	@Override
	public H group(Class<?>... groups) {
		H next = this.clone();
		next.base = base.group(groups);
		return next;
	}

	@Override
	public <E> LoadType<E> type(Class<E> type) {
		return base.type(type);
	}

	@Override
	public <E> Ref<E> ref(Ref<E> ref) {
		return base.ref(ref);
	}

	@Override
	public <E> Map<Key<E>, E> refs(Iterable<Ref<E>> refs) {
		return base.refs(refs);
	}

	@Override
	public <E> Map<Key<E>, E> refs(Ref<? extends E>... refs) {
		return base.refs(refs);
	}

	@Override
	public <E> Ref<E> key(Key<E> key) {
		return base.key(key);
	}

	@Override
	public <E> Map<Key<E>, E> keys(Iterable<Key<E>> keys) {
		return base.keys(keys);
	}

	@Override
	public <E> Map<Key<E>, E> keys(Key<E>... keys) {
		return base.keys(keys);
	}

	@Override
	public <E> Ref<E> entity(E entity) {
		return base.entity(entity);
	}

	@Override
	public <E> Map<Key<E>, E> entities(Iterable<E> entities) {
		return base.entities(entities);
	}

	@Override
	public <E> Map<Key<E>, E> entities(E... entities) {
		return base.entities(entities);
	}

	@Override
	public <E> Ref<E> value(Object key) {
		return base.value(key);
	}

	@Override
	public <E> Map<Key<E>, E> values(Iterable<?> keysOrEntities) {
		return base.values(keysOrEntities);
	}

	@Override
	public <E> Map<Key<E>, E> values(Object... keysOrEntities) {
		return base.values(keysOrEntities);
	}

	@Override
	public Objectify getObjectify() {
		return base.getObjectify();
	}

	@Override
	public Set<Class<?>> getLoadGroups() {
		return base.getLoadGroups();
	}

	@Override
	public void setWrapper(Loader loader) {
		base.setWrapper(loader);
	}
	
}
