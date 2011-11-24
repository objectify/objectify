package com.googlecode.objectify.impl.load;

import com.googlecode.objectify.impl.Path;

/**
 * <p>Helper for translators; it's handy to have a path associated with them.</p>
 */
abstract public class AbstractTranslator<T> implements Translator<T>
{
	/** */
	protected Path path;
	
	/** */
	public AbstractTranslator(Path path) {
		this.path = path;
	}
}
