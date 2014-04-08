/*
 */

package com.googlecode.objectify.test.util;

import com.googlecode.objectify.impl.LoadConditions;
import com.googlecode.objectify.impl.LoadEngine;
import com.googlecode.objectify.impl.translate.LoadContext;
import org.mockito.Matchers;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Simplifies the load context just for testing
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class FakeLoadContext extends LoadContext
{
	private static LoadEngine mockLoadEngine() {
		LoadEngine mock = mock(LoadEngine.class);
		when(mock.shouldLoad(Matchers.<LoadConditions>any())).thenReturn(false);
		return mock;
	}

	public FakeLoadContext() {
		super(null, mockLoadEngine());
	}
}
