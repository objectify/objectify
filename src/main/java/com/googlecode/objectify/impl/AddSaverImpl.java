package com.googlecode.objectify.impl;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.Result;
import java.util.Map;

/**
 * Implementation of the Add/Create in datastore
 */
public class AddSaverImpl extends SaverImpl {

  /** */
  public AddSaverImpl(ObjectifyImpl ofy) {
    super(ofy);
  }

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Saver#entities(java.lang.Iterable)
	 */
  @Override
  public <E> Result<Map<Key<E>, E>> entities(final Iterable<E> entities) {
    return ofy.createWriteEngine().create(entities);
  }

}
