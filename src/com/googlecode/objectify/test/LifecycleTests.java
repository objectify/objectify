package com.googlecode.objectify.test;

import javax.persistence.Id;
import javax.persistence.PostLoad;
import javax.persistence.PrePersist;

import org.testng.annotations.Test;

import com.google.appengine.api.datastore.Entity;
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
		boolean prePersistedWithParam;
		boolean postLoaded;
		boolean postLoadedWithParam;

		@PrePersist void prePersist() { this.prePersisted = true; }
		@PrePersist void prePersist(Entity ent) { this.prePersistedWithParam = true; }
		@PostLoad void postLoad() { this.postLoaded = true; }
		@PostLoad void postLoad(Entity ent) { this.postLoadedWithParam = true; }
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
		assert fetched.prePersistedWithParam;
		assert fetched.postLoaded;	// will fail with caching objectify, this is ok
		assert fetched.postLoadedWithParam;

		HasLifecycle life2 = new HasInheritedLifecycle();
		fetched = this.putAndGet(life2);
		
		assert fetched.prePersisted;
		assert fetched.prePersistedWithParam;
		assert fetched.postLoaded;	// will fail with caching objectify, this is ok
		assert fetched.postLoadedWithParam;
	}
}
