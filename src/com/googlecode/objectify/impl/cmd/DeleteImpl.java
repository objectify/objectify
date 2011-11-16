package com.googlecode.objectify.impl.cmd;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.Result;
import com.googlecode.objectify.cmd.Delete;
import com.googlecode.objectify.cmd.DeleteType;


/**
 * Implementation of the Delete command.
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
class DeleteImpl implements Delete
{
	/** */
	ObjectifyImpl ofy;
	
	/** */
	DeleteImpl(ObjectifyImpl ofy) {
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
	public Result<Void> entity(Object keyOrEntity) {
		return entities(Collections.singleton(keyOrEntity));
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Delete#values(java.lang.Object[])
	 */
	@Override
	public Result<Void> entities(Object... keysOrEntities) {
		return entities(Arrays.asList(keysOrEntities));
	}
	
	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Delete#values(java.lang.Iterable)
	 */
	@Override
	public Result<Void> entities(Iterable<?> keysOrEntities) {
		List<com.google.appengine.api.datastore.Key> keys = ofy.getFactory().getRawKeys(keysOrEntities);
		return ofy.delete(keys);
	}
}
