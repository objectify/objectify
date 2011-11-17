package com.googlecode.objectify.test.util;

import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.ObjectifyOpts;

/**
 * Primarily exists to enable the TestObjectify
 */
public class TestObjectifyFactory extends ObjectifyFactory
{
	@Override
	public TestObjectify begin()
	{
		return new TestObjectify(super.begin());
	}
	
	@Override
	public TestObjectify beginTransaction()
	{
		return new TestObjectify(super.beginTransaction());
	}
	
	@Override
	protected ObjectifyOpts createDefaultOpts()
	{
		ObjectifyOpts opts = super.createDefaultOpts();
		// This can be used to enable/disable the memory cache globally.
		opts = opts.globalCache(true);
		
		// This can be used to enable/disable the session caching objectify
		// Note that it will break several unit tests that check for transmutation
		// when entities are run through the DB (ie, unknown List types become
		// ArrayList).  These failures are ok.
		opts = opts.sessionCache(false);
		
		return opts;
	}
	
}
