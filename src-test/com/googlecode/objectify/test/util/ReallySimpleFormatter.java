/*
 */

package com.googlecode.objectify.test.util;

import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

/**
 * A formatter that makes it easier to read log messages
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class ReallySimpleFormatter extends SimpleFormatter
{
	static final int MESSAGE_COLUMN = 60;
	
	@Override
	public synchronized String format(LogRecord record)
	{
		StringBuilder bld = new StringBuilder();
		
		// Log level
		bld.append(record.getLevel()).append(":  ");
		
		// Class.method using shortName
		String className = record.getSourceClassName();
		className = className.substring(className.lastIndexOf('.')+1);
		bld.append(className).append('.').append(record.getSourceMethodName()).append(":  ");
		
		if (bld.length() < MESSAGE_COLUMN)
			while (bld.length() < MESSAGE_COLUMN)
				bld.append(' ');
		else
			bld.append('\t');
		
		// The message
		bld.append(record.getMessage());

		// Newline
		bld.append('\n');
		
		return bld.toString();
	}
}
