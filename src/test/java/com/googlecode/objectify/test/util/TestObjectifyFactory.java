package com.googlecode.objectify.test.util;

import com.googlecode.objectify.ObjectifyFactory;

/**
 * Primarily exists to enable the TestObjectify
 */
public class TestObjectifyFactory extends ObjectifyFactory
{
	public TestObjectifyFactory() {
		super(false);
	}
	
	@Override
	public TestObjectify begin()
	{
		return new TestObjectify(this)

			// This can be used to enable/disable the memory cache globally.
			.cache(true);
	}
}
