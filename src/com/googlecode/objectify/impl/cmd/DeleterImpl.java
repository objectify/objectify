package com.googlecode.objectify.impl.cmd;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.Result;
import com.googlecode.objectify.cmd.Deleter;
import com.googlecode.objectify.cmd.DeleteType;


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
	 * @see com.googlecode.objectify.cmd.Delete#value(java.lang.Object)
	 */
	@Override
	public Result<Void> key(Object keyOrEntity) {
		return keys(Collections.singleton(keyOrEntity));
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Delete#values(java.lang.Object[])
	 */
	@Override
	public Result<Void> keys(Object... keysOrEntities) {
		return keys(Arrays.asList(keysOrEntities));
	}
	
	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Delete#values(java.lang.Iterable)
	 */
	@Override
	public Result<Void> keys(Iterable<?> keysOrEntities) {
		
		List<com.google.appengine.api.datastore.Key> keys = new ArrayList<com.google.appengine.api.datastore.Key>();
		for (Object obj: keysOrEntities)
			keys.add(ofy.getFactory().getRawKey(obj));

		return ofy.createWriteEngine().delete(keys);
	}
}
