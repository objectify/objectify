package com.googlecode.objectify.impl.translate;

import com.google.appengine.api.datastore.PropertyContainer;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.impl.Path;

/** 
 * The context while creating translator factories. Tracks important state as we navigate the class graph.
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class CreateContext
{
	/** The objectify factory instance */
	ObjectifyFactory factory;
	public ObjectifyFactory getFactory() { return this.factory; }
	
	/** */
	public CreateContext(ObjectifyFactory fact) {
		this.factory = fact;
	}

	/**
	 * Get the relevant translator, creating it if necessary.
	 */
	public <P, D> Translator<P, D> getTranslator(TypeKey<P> tk, CreateContext ctx, Path path) {
		return factory.getTranslators().get(tk, ctx, path);
	}

	/**
	 * Get the populator for the specified class. This requires looking up the
	 * translator for that class and then getting the populator from it.
	 *
	 * @param clazz is the class we want a populator for.
	 */
	@SuppressWarnings("unchecked")
	public <P> Populator<P> getPopulator(Class<P> clazz, Path path) {
		if (clazz == null || clazz.equals(Object.class)) {
			return (Populator<P>)NullPopulator.INSTANCE;
		} else {
			ClassTranslator<P> classTranslator = (ClassTranslator<P>)this.<P, PropertyContainer>getTranslator(new TypeKey<P>(clazz), this, path);
			return classTranslator.getPopulator();
		}
	}
}