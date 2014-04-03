package com.googlecode.objectify.impl.translate;

import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.impl.Path;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

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
	public <P, D> Translator<P, D> getTranslator(Type type, Annotation[] annotations, CreateContext ctx, Path path) {
		return factory.getTranslators().get(type, annotations, ctx, path);
	}

	/**
	 * Get the populator for the specified class. This requires looking up the
	 * translator for that class and then getting the populator from it.
	 *
	 * @param clazz is the class we want a populator for.
	 */
	@SuppressWarnings("unchecked")
	public <P> Populator<P> getPopulator(Class<P> clazz, Path path) {
		if (clazz.equals(Object.class)) {
			return (Populator<P>)NullPopulator.INSTANCE;
		} else {
			HasPopulator<P> hasPopulator = (HasPopulator<P>)getTranslator(clazz, new Annotation[0], this, path);
			return hasPopulator.getPopulator();
		}
	}
}