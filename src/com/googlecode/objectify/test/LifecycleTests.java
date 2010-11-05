package com.googlecode.objectify.test;

import javax.persistence.Id;
import javax.persistence.PostLoad;
import javax.persistence.PrePersist;

import org.testng.annotations.Test;

import com.google.appengine.api.datastore.Entity;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.annotation.Cached;

/**
 * Tests the lifecycle annotations
 */
public class LifecycleTests extends TestBase
{
	@Cached
	public static class HasLifecycle
	{
		@Id Long id;
		boolean prePersisted;
		boolean prePersistedWithObjectify;
		boolean prePersistedWithEntity;
		boolean prePersistedWithBoth;
		boolean postLoaded;
		boolean postLoadedWithObjectify;
		boolean postLoadedWithEntity;
		boolean postLoadedWithBoth;

		@PrePersist void prePersist() { this.prePersisted = true; }
		@PrePersist void prePersist(Objectify ofy) { this.prePersistedWithObjectify = true; }
		@PrePersist void prePersist(Entity ent) { this.prePersistedWithEntity = true; }
		@PrePersist void prePersist(Objectify ofy, Entity ent) { this.prePersistedWithBoth = true; }
		@PostLoad void postLoad() { this.postLoaded = true; }
		@PostLoad void postLoad(Objectify ofy) { this.postLoadedWithObjectify = true; }
		@PostLoad void postLoad(Entity ent) { this.postLoadedWithEntity = true; }
		@PostLoad void postLoad(Objectify ofy, Entity ent) { this.postLoadedWithBoth = true; }
	}

	@Cached
	public static class HasInheritedLifecycle extends HasLifecycle {}

	/** */
	@Test
	public void testLifecycle() throws Exception
	{
		this.fact.register(HasLifecycle.class);
		this.fact.register(HasInheritedLifecycle.class);
		
		HasLifecycle life1 = new HasLifecycle();
		HasLifecycle fetched = this.putAndGet(life1);
		
		assert fetched.prePersisted;
		assert fetched.prePersistedWithObjectify;
		assert fetched.prePersistedWithEntity;
		assert fetched.prePersistedWithBoth;
		assert fetched.postLoaded;	// will fail with caching objectify, this is ok
		assert fetched.postLoadedWithObjectify;
		assert fetched.postLoadedWithEntity;
		assert fetched.postLoadedWithBoth;

		HasLifecycle life2 = new HasInheritedLifecycle();
		fetched = this.putAndGet(life2);
		
		assert fetched.prePersisted;
		assert fetched.prePersistedWithObjectify;
		assert fetched.prePersistedWithEntity;
		assert fetched.prePersistedWithBoth;
		assert fetched.postLoaded;	// will fail with caching objectify, this is ok
		assert fetched.postLoadedWithObjectify;
		assert fetched.postLoadedWithEntity;
		assert fetched.postLoadedWithBoth;
	}
	
	@Cached
	public static class HasExceptionThrowingLifecycle
	{
		@Id Long id;
		@PrePersist void prePersist() { throw new UnsupportedOperationException(); }
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
