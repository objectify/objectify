package com.googlecode.objectify.impl.cmd;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.Result;
import com.googlecode.objectify.cmd.DeleteType;
import com.googlecode.objectify.cmd.Deleter;


/**
 * Implementation of the Delete command.
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
class DeleterImpl implements Deleter
{
	/** */
	ObjectifyImpl ofy;
	
	/** */
	DeleterImpl(ObjectifyImpl ofy) {
		this.ofy = ofy;
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Delete#type(java.lang.Class)
	 */
	@Override
	public DeleteType type(Class<?> type) {
		String kind = Key.getKind(type);
		return new DeleteTypeImpl(ofy, kind);
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Deleter#key(com.googlecode.objectify.Key)
	 */
	@Override
	public Result<Void> key(Key<?> key) {
		return keys(key);
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Deleter#keys(com.googlecode.objectify.Key<?>[])
	 */
	@Override
	public Result<Void> keys(Key<?>... keys) {
		return keys(Arrays.asList(keys));
	}
	
	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Delete#values(java.lang.Iterable)
	 */
	@Override
	public Result<Void> keys(Iterable<Key<?>> keys) {
		List<com.google.appengine.api.datastore.Key> rawKeys = new ArrayList<com.google.appengine.api.datastore.Key>();
		for (Key<?> key: keys)
			rawKeys.add(key.getRaw());

		return ofy.createWriteEngine().delete(rawKeys);
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Deleter#entity(java.lang.Object)
	 */
	@Override
	public Result<Void> entity(Object entity) {
		return entities(entity);
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Deleter#entities(java.lang.Iterable)
	 */
	@Override
	public Result<Void> entities(Iterable<?> entities) {
		List<com.google.appengine.api.datastore.Key> keys = new ArrayList<com.google.appengine.api.datastore.Key>();
		for (Object obj: entities)
			keys.add(ofy.getFactory().getRawKey(obj));

		return ofy.createWriteEngine().delete(keys);
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Deleter#entities(java.lang.Object[])
	 */
	@Override
	public Result<Void> entities(Object... entities) {
		return entities(Arrays.asList(entities));
	}
}
