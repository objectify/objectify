package com.googlecode.objectify.impl.translate;

import com.google.appengine.api.datastore.EmbeddedEntity;
import com.google.appengine.api.datastore.PropertyContainer;
import com.googlecode.objectify.impl.Path;
import com.googlecode.objectify.util.LogUtils;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <p>Translator which knows how to convert an embedded POJO type into an EmbeddedEntity.</p>
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class EmbeddedClassTranslator<P> extends AbstractClassTranslator<P>
{
	private static final Logger log = Logger.getLogger(EmbeddedClassTranslator.class.getName());

	/** */
	public EmbeddedClassTranslator(Class<P> clazz, CreateContext ctx, Path path) {
		super(clazz, ctx, path, new ClassPopulator<P>(clazz, ctx, path));
	}

	/* */
	@Override
	public P loadSafe(PropertyContainer container, LoadContext ctx, Path path) throws SkipException {
		if (log.isLoggable(Level.FINEST))
			log.finest(LogUtils.msg(path, "Instantiating a " + clazz.getName()));

		P into = fact.construct(clazz);

		populator.load(container, ctx, path, into);
		
		return into;
	}

	/* */
	@Override
	public PropertyContainer saveSafe(P pojo, boolean index, SaveContext ctx, Path path) throws SkipException {
		PropertyContainer into = new EmbeddedEntity();

		populator.save(pojo, index, ctx, path, into);

		return into;
	}
}
