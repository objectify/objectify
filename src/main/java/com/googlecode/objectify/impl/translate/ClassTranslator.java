package com.googlecode.objectify.impl.translate;

import com.google.appengine.api.datastore.EmbeddedEntity;
import com.google.appengine.api.datastore.PropertyContainer;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.impl.Path;
import com.googlecode.objectify.impl.PropertyPopulator;
import com.googlecode.objectify.util.LogUtils;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <p>Translator which knows how to convert a POJO type into a PropertiesContainer.</p>
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class ClassTranslator<P> extends NullSafeTranslator<P, PropertyContainer>
{
	private static final Logger log = Logger.getLogger(ClassTranslator.class.getName());

	/** */
	private final ObjectifyFactory fact;

	/** The declared class we are responsible for. */
	protected final Class<P> clazz;

	/** Does the heavy lifting of copying properties */
	private final Populator<P> populator;

	/** */
	public ClassTranslator(Class<P> clazz, CreateContext ctx, Path path) {
		if (log.isLoggable(Level.FINEST))
			log.finest("Creating class translator for " + clazz.getName() + " at path '"+ path + "'");

		this.fact = ctx.getFactory();
		this.clazz = clazz;

		this.populator = new ClassPopulator<>(clazz, ctx, path);
	}

	/**
	 * Called when each property is discovered, allows a subclass to do something special with it
	 * @return false if the property should not be considered a standard populated property.
	 */
	protected boolean consider(PropertyPopulator<Object, Object> tprop) { return true; }

	/* */
	@Override
	public P loadSafe(PropertyContainer container, LoadContext ctx, Path path, P into) throws SkipException {

		if (log.isLoggable(Level.FINEST))
			log.finest(LogUtils.msg(path, "Instantiating a " + clazz.getName()));

		into = constructEmptyPojo(container, ctx, path);

		populator.load(container, ctx, path, into);
		
		return into;
	}

	/* */
	@Override
	public PropertyContainer saveSafe(P pojo, boolean index, SaveContext ctx, Path path) throws SkipException {
		PropertyContainer into = constructEmptyContainer(pojo, path);

		populator.save(pojo, index, ctx, path, into);

		return into;
	}

	/**
	 * Construct an empty container for the properties of the pojo. Subclasses of this translator
	 * may wish to initialize key fields.
	 */
	protected PropertyContainer constructEmptyContainer(P pojo, Path path) {
		return new EmbeddedEntity();
	}

	/**
	 * Construct the empty POJO. Subclasses of this translator may wish to initialize key fields.
	 */
	protected P constructEmptyPojo(PropertyContainer container, LoadContext ctx, Path path) {
		return fact.construct(clazz);
	}
}
