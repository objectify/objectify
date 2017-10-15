package com.googlecode.objectify.test.util;

import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.mockito.MockitoAnnotations;

/**
 */
public class MockitoExtension implements BeforeEachCallback {

	@Override
	public void beforeEach(final ExtensionContext context) throws Exception {
		final Object testInstance = context.getTestInstance().get();

		MockitoAnnotations.initMocks(testInstance);
	}
}
