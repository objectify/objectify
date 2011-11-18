package com.googlecode.objectify.test.util;

import com.googlecode.objectify.ObjectifyFactory;

/**
 * Primarily exists to enable the TestObjectify
 */
public class TestObjectifyFactory extends ObjectifyFactory
{
	@Override
	public TestObjectify begin()
	{
		return new TestObjectify(super.begin())
		
			// This can be used to enable/disable the memory cache globally.
			.globalCache(true)
			
			// This can be used to enable/disable the session caching objectify
			// Note that it will break several unit tests that check for transmutation
			// when entities are run through the DB (ie, unknown List types become
			// ArrayList).  These failures are ok.
			.sessionCache(false);
	}
}
