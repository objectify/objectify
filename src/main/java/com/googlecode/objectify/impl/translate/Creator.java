package com.googlecode.objectify.impl.translate;

import com.google.appengine.api.datastore.EmbeddedEntity;
import com.google.appengine.api.datastore.PropertyContainer;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.impl.Forge;
import com.googlecode.objectify.impl.Path;
import com.googlecode.objectify.util.LogUtils;

import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * <p>Factory for POJO and PropertyContainer objects. Lets us hide the distinction between
 * Entity creation and embedded object creation.</p>
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
abstract public class Creator<P> implements Translator<P, PropertyContainer>
{
	private static final Logger log = Logger.getLogger(Creator.class.getName());

	Class<P> clazz;
	Forge forge;

	/**
	 */
	public Creator(Class<P> clazz, Forge forge) {
		this.clazz = clazz;
		this.forge = forge;
	}

	/**
	 * Make an instance of the thing
	 */
	protected P construct(Path path) {
		if (log.isLoggable(Level.FINEST))
			log.finest(LogUtils.msg(path, "Instantiating a " + clazz.getName()));

		return forge.construct(clazz);
	}
}
