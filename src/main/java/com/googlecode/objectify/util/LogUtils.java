/*
 */

package com.googlecode.objectify.util;

import com.googlecode.objectify.impl.Path;

/**
 * <p>Static methods that help out making log messages.</p>
 * 
 * @author Jeff Schnitzer
 */
public class LogUtils
{
	/** */
	static final int PATH_PADDING = 13;

	private LogUtils() {
	}

	/** Create a log a message for a given path */
	public static String msg(Path path, String msg) {
		StringBuilder bld = new StringBuilder();
		bld.append("\t.");
		bld.append(path.toPathString());
		
		if (bld.length() < PATH_PADDING)
			while (bld.length() < PATH_PADDING)
				bld.append(' ');
		else
			bld.append('\t');

		bld.append(msg);
		
		return bld.toString();
	}
}