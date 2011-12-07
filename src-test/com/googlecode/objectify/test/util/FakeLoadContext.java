/*
 */

package com.googlecode.objectify.test.util;

import com.googlecode.objectify.impl.Property;
import com.googlecode.objectify.impl.engine.LoadEngine;
import com.googlecode.objectify.impl.translate.LoadContext;

/**
 * Simplifies the load context just for testing
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class FakeLoadContext extends LoadContext
{
	public FakeLoadContext() {
		super(null, new LoadEngine(null, null, null, null) {
			@Override
			public boolean shouldLoad(Property prop) {
				return false;
			}
		});
	}
}
