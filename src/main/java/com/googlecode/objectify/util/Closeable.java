package com.googlecode.objectify.util;

/**
 * Get rid of the stupid checked exception
 */
public interface Closeable extends java.io.Closeable {
	@Override void close();
}
