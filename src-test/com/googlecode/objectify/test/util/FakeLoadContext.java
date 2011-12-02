/*
 */

package com.googlecode.objectify.test.util;

import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Load;
import com.googlecode.objectify.impl.translate.LoadContext;

/**
 * Simplifies the load context just for testing
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class FakeLoadContext extends LoadContext
{
	public FakeLoadContext() {
		super(null, null);
	}
	
	@Override
	public void maybeLoadRef(Load load, Ref<?> ref) {
		// do nothing
	}
}
