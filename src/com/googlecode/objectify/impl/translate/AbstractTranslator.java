package com.googlecode.objectify.impl.translate;

import com.googlecode.objectify.impl.Path;

/**
 * <p>Helper for translators; it's handy to have a path associated with them.</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
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
