package com.googlecode.objectify.impl.load;

import com.googlecode.objectify.impl.Path;

/**
 * <p>Helper for loaders.</p>
 */
abstract public class LoaderAbstract<T> implements Loader<T>
{
	/** */
	protected Path path;
	
	/** */
	public LoaderAbstract(Path path) {
		this.path = path;
	}
}
