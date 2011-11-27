package com.googlecode.objectify.test;

import org.testng.annotations.Test;

import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.OnLoad;
import com.googlecode.objectify.annotation.OnSave;
import com.googlecode.objectify.test.util.TestBase;

/**
 * Tests the lifecycle annotations
 */
public class LifecycleTests extends TestBase
{
	@com.googlecode.objectify.annotation.Entity
	@Cache
	public static class HasLifecycle
	{
		@Id Long id;
		boolean onSaved;
		boolean onSavedWithObjectify;
		boolean onLoaded;
		boolean onLoadedWithObjectify;

		@OnSave void onSave() { this.onSaved = true; }
		@OnSave void onSave(Objectify ofy) { this.onSavedWithObjectify = true; }
		@OnLoad void onLoad() { this.onLoaded = true; }
		@OnLoad void onLoad(Objectify ofy) { this.onLoadedWithObjectify = true; }
	}

	@com.googlecode.objectify.annotation.Entity
	@Cache
	public static class HasInheritedLifecycle extends HasLifecycle {}

	/** */
	@Test
	public void testLifecycle() throws Exception
	{
		this.fact.register(HasLifecycle.class);
		this.fact.register(HasInheritedLifecycle.class);
		
		HasLifecycle life1 = new HasLifecycle();
		HasLifecycle fetched = this.putAndGet(life1);
		
		assert fetched.onSaved;
		assert fetched.onSavedWithObjectify;
		assert fetched.onLoaded;	// would fail without session clear
		assert fetched.onLoadedWithObjectify;

		HasLifecycle life2 = new HasInheritedLifecycle();
		fetched = this.putAndGet(life2);
		
		assert fetched.onSaved;
		assert fetched.onSavedWithObjectify;
		assert fetched.onLoaded;	// would fail without session clear
		assert fetched.onLoadedWithObjectify;
	}
	
	@com.googlecode.objectify.annotation.Entity
	@Cache
	public static class HasExceptionThrowingLifecycle
	{
		@Id Long id;
		@OnSave void onSave() { throw new UnsupportedOperationException(); }
	}

	/** */
	@Test
	public void testExceptionInLifecycle() throws Exception
	{
		this.fact.register(HasExceptionThrowingLifecycle.class);
		
		try
		{
			this.putAndGet(new HasExceptionThrowingLifecycle());
			assert false;
		}
		catch (UnsupportedOperationException ex)
		{
			// this is correct
		}
	}
}
