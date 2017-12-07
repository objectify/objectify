package com.googlecode.objectify.impl.translate;

import com.google.cloud.datastore.FullEntity;
import com.googlecode.objectify.impl.Forge;
import com.googlecode.objectify.impl.Path;
import com.googlecode.objectify.impl.PropertyContainer;
import com.googlecode.objectify.impl.SavePropertyContainer;


/**
 * <p>This version is for creating normal embedded objects.</p>
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class EmbeddedCreator<P> extends Creator<P>
{
	/**
	 */
	public EmbeddedCreator(final Class<P> clazz, final Forge forge) {
		super(clazz, forge);
	}

	@Override
	public P load(final PropertyContainer node, final LoadContext ctx, final Path path) throws SkipException {
		return construct(path);
	}

	@Override
	public PropertyContainer save(final P pojo, final SaveContext ctx, final Path path) throws SkipException {
		return new SavePropertyContainer(FullEntity.newBuilder());
	}
}
