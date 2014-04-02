package com.googlecode.objectify.stringifier;


import com.googlecode.objectify.Key;

/**
 * <p>Converts Objectify Key<?>s to String.</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class KeyStringifier implements Stringifier<Key<?>>
{
	@Override
	public String toString(Key<?> obj) {
		return obj.getString();
	}

	@Override
	public Key<?> fromString(String str) {
		return Key.create(str);
	}
}