package com.googlecode.objectify.impl.translate;

import com.googlecode.objectify.impl.Path;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

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
//	/**
//	 * Determine if this factory should be used to create a translator in this situation. If true is returned,
//	 * create() will be called.
//	 */
//	boolean applies(Path path, Property property, Type type, CreateContext ctx);

	/**
//	 * Called if applies() returns true to indicate that a translator should be used. Also called if the
//	 * translator is explicitly specified with the @Translate annotation.
	 *
	 * Create a translator for a type.
	 *
	 * @param type is the generic type of the field (or field component).  For example, examining a field of type
	 *  List<String> will have a Type of List<String>; the TranslatorFactory which recognizes List may then ask
	 *  for a translator of its component type String.
	 * @param annotations are any additional annotations relevant to the occurrence of this type. If this translator
	 *  is on a field, it will be the annotations of the field. For root entity translators, this will be an empty array.
	 * @param path is where this type was discovered, important for logging and exceptions
	 * @return null if this factory does not know how to deal with that situation.
	 */
	Translator<P, D> create(Type type, Annotation[] annotations, CreateContext ctx, Path path);
}
