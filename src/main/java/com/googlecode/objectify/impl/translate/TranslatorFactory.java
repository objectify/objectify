package com.googlecode.objectify.impl.translate;

import com.googlecode.objectify.impl.Path;

/**
 * <p>A translator knows how to convert between a POJO and a native datastore representation. When an entity class
 * is registered, the known TranslatorFactories are queried to produce a translator for that class; this translator
 * will in turn be composed of translators for all of the fields, etc. These translators become a static metamodel
 * that can efficiently convert back and forth between the formats with minimal runtime overhead.</p>
 * 
 * <p>P is the pojo type, D is the datastore type.</p>
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public interface TranslatorFactory<P, D>
{
	/**
	 * Create a translator for a type.
	 *
	 * @param tk defines the type which is to be translated
	 * @param path is where this type was discovered, important for logging and exceptions
	 * @return null if this factory does not know how to deal with that situation.
	 */
	Translator<P, D> create(TypeKey<P> tk, CreateContext ctx, Path path);
}
