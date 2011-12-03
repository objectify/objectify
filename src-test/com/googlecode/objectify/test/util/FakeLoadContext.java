/*
 */

package com.googlecode.objectify.test.util;

import com.googlecode.objectify.annotation.Load;
import com.googlecode.objectify.impl.engine.LoadBatch;
import com.googlecode.objectify.impl.translate.LoadContext;

/**
 * Simplifies the load context just for testing
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class FakeLoadContext extends LoadContext
{
	public FakeLoadContext() {
		super(null, new LoadBatch(null, null, null, null) {
			@Override
			public boolean shouldLoad(Load load) {
				return false;
			}
		});
	}
}
