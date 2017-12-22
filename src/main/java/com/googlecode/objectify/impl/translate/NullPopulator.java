package com.googlecode.objectify.impl.translate;

import com.google.cloud.datastore.FullEntity;
import com.googlecode.objectify.impl.Path;


/**
 * <p>Populator which does nothing.</p>
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class NullPopulator implements Populator<Object>
{
	public static NullPopulator INSTANCE = new NullPopulator();

	@Override
	public void load(FullEntity<?> node, LoadContext ctx, Path path, Object into) {
	}

	@Override
	public void save(Object pojo, boolean index, SaveContext ctx, Path path, FullEntity.Builder<?> into) {
	}
}
