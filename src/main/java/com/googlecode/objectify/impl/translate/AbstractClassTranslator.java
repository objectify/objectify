package com.googlecode.objectify.impl.translate;

import com.google.appengine.api.datastore.PropertyContainer;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.impl.Path;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <p>Some common code for Translators which know how to convert a POJO type into a PropertiesContainer.</p>
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
abstract public class AbstractClassTranslator<P> extends NullSafeTranslator<P, PropertyContainer> implements HasPopulator<P>
{
	private static final Logger log = Logger.getLogger(AbstractClassTranslator.class.getName());

	/** */
	protected final ObjectifyFactory fact;

	/** The declared class we are responsible for. */
	protected final Class<P> clazz;

	/** Does the heavy lifting of copying properties */
	protected final Populator<P> populator;

	/** */
	public AbstractClassTranslator(Class<P> clazz, CreateContext ctx, Path path, ClassPopulator<P> populator) {
		if (log.isLoggable(Level.FINEST))
			log.finest("Creating class translator for " + clazz.getName() + " at path '"+ path + "'");

		this.fact = ctx.getFactory();
		this.clazz = clazz;
		this.populator = populator;
	}

	/**
	 * Get the populator associated with this class.
	 */
	@Override
	public Populator<P> getPopulator() {
		return populator;
	}
}
