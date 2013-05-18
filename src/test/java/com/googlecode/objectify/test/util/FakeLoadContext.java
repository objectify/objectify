/*
 */

package com.googlecode.objectify.test.util;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mockito.Matchers;

import com.googlecode.objectify.impl.LoadEngine;
import com.googlecode.objectify.impl.Property;
import com.googlecode.objectify.impl.translate.LoadContext;

/**
 * Simplifies the load context just for testing
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class FakeLoadContext extends LoadContext
{
	private static LoadEngine mockLoadEngine() {
		LoadEngine mock = mock(LoadEngine.class);
		when(mock.shouldLoad(Matchers.<Property>any())).thenReturn(false);
		return mock;
	}

	public FakeLoadContext() {
		super(null, mockLoadEngine());
	}
}
