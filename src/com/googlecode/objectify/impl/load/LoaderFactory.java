package com.googlecode.objectify.impl.load;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import com.googlecode.objectify.ObjectifyFactory;

/**
 * 
 * <p>A loader knows how to load a subtree of a datastore entity into a POJO object.</p>
 * 
 * <p>Loaders are composed of other loaders; through a chain of these a whole entity
 * object is assembled.</p>
 *
 * <p>Before the load process is started, the Entity must be broken down into an EntityNode
 * (which itself may be composed of EntityNodes).  This allows each Loader to process only
 * the piece of the Entity it cares about.</p>
 */
public interface LoaderFactory<T>
{
	/**
	 * @return null if this factory does not know how to deal with that situation. 
	 */
	Loader<T> create(ObjectifyFactory fact, Type type, Annotation[] fieldAnnotations);
}
