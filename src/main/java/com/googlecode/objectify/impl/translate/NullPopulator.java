package com.googlecode.objectify.impl.translate;

import com.googlecode.objectify.impl.Path;
import com.googlecode.objectify.impl.PropertyContainer;


/**
 * <p>Populator which does nothing.</p>
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class NullPopulator implements Populator<Object>
{
	public static NullPopulator INSTANCE = new NullPopulator();

	@Override
	public void load(PropertyContainer node, LoadContext ctx, Path path, Object into) {
	}

	@Override
	public void save(Object pojo, boolean index, SaveContext ctx, Path path, PropertyContainer into) {
	}
}
