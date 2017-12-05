package com.googlecode.objectify.impl.translate;

import com.google.cloud.datastore.FullEntity;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.impl.Path;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 
 * The context while creating translator factories. Tracks important state as we navigate the class graph.
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
@RequiredArgsConstructor
public class CreateContext
{
	/** */
	@Getter
	private final ObjectifyFactory factory;

	/**
	 * Get the relevant translator, creating it if necessary.
	 */
	public <P, D> Translator<P, D> getTranslator(final TypeKey<P> tk, final CreateContext ctx, final Path path) {
		return factory.getTranslators().get(tk, ctx, path);
	}

	/**
	 * Get the populator for the specified class. This requires looking up the
	 * translator for that class and then getting the populator from it.
	 *
	 * @param clazz is the class we want a populator for.
	 */
	@SuppressWarnings("unchecked")
	public <P> Populator<P> getPopulator(final Class<P> clazz, final Path path) {
		if (clazz == null || clazz.equals(Object.class)) {
			return (Populator<P>)NullPopulator.INSTANCE;
		} else {
			final ClassTranslator<P> classTranslator = (ClassTranslator<P>)this.<P, FullEntity<?>>getTranslator(new TypeKey<>(clazz), this, path);
			return classTranslator.getPopulator();
		}
	}
}