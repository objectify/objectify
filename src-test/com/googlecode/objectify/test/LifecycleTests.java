package com.googlecode.objectify.test;

import org.testng.annotations.Test;

import com.google.appengine.api.datastore.Entity;
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
		boolean onSavedWithEntity;
		boolean onSavedWithBoth;
		boolean onLoaded;
		boolean onLoadedWithObjectify;
		boolean onLoadedWithEntity;
		boolean onLoadedWithBoth;

		@OnSave void onSave() { this.onSaved = true; }
		@OnSave void onSave(Objectify ofy) { this.onSavedWithObjectify = true; }
		@OnSave void onSave(Entity ent) { this.onSavedWithEntity = true; }
		@OnSave void onSave(Objectify ofy, Entity ent) { this.onSavedWithBoth = true; }
		@OnLoad void onLoad() { this.onLoaded = true; }
		@OnLoad void onLoad(Objectify ofy) { this.onLoadedWithObjectify = true; }
		@OnLoad void onLoad(Entity ent) { this.onLoadedWithEntity = true; }
		@OnLoad void onLoad(Objectify ofy, Entity ent) { this.onLoadedWithBoth = true; }
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
		assert fetched.onSavedWithEntity;
		assert fetched.onSavedWithBoth;
		assert fetched.onLoaded;	// will fail with caching objectify, this is ok
		assert fetched.onLoadedWithObjectify;
		assert fetched.onLoadedWithEntity;
		assert fetched.onLoadedWithBoth;

		HasLifecycle life2 = new HasInheritedLifecycle();
		fetched = this.putAndGet(life2);
		
		assert fetched.onSaved;
		assert fetched.onSavedWithObjectify;
		assert fetched.onSavedWithEntity;
		assert fetched.onSavedWithBoth;
		assert fetched.onLoaded;	// will fail with caching objectify, this is ok
		assert fetched.onLoadedWithObjectify;
		assert fetched.onLoadedWithEntity;
		assert fetched.onLoadedWithBoth;
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
