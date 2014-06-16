package com.googlecode.objectify.stringifier;

import com.googlecode.objectify.ObjectifyFactory;

import java.lang.reflect.Type;

/**
 * <p>If a Stringifier<?> implements this interface, it will be called once just after construction
 * with the actual Type of the key to stringify.</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public interface InitializeStringifier
{
	/**
	 * Informs the stringifier of the actual key type.
	 *
	 * @param fact is just handy to have around
	 * @param keyType is the declared type of the map key.
	 */
	void init(ObjectifyFactory fact, Type keyType);
}