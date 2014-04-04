package com.googlecode.objectify.impl.translate;

import com.google.appengine.api.datastore.EmbeddedEntity;
import com.google.appengine.api.datastore.PropertyContainer;
import com.googlecode.objectify.impl.Forge;
import com.googlecode.objectify.impl.Path;


/**
 * <p>This version is for creating normal embedded objects.</p>
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class EmbeddedCreator<P> extends Creator<P>
{
	/**
	 */
	public EmbeddedCreator(Class<P> clazz, Forge forge) {
		super(clazz, forge);
	}

	@Override
	public P load(PropertyContainer node, LoadContext ctx, Path path) throws SkipException {
		return construct(path);
	}

	@Override
	public PropertyContainer save(P pojo, boolean index, SaveContext ctx, Path path) throws SkipException {
		return new EmbeddedEntity();
	}
}
