package com.googlecode.objectify.impl;

import java.util.HashMap;

import com.googlecode.objectify.Key;

/**
 * The basic session cache.  A lot easier than passing the generic arguments around!
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class Session extends HashMap<Key<?>, SessionEntity>
{
	private static final long serialVersionUID = 1L;
}
