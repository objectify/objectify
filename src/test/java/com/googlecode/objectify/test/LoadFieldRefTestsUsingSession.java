/*
 */

package com.googlecode.objectify.test;

import java.util.ArrayList;
import java.util.List;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Load;
import com.googlecode.objectify.test.LoadFieldRefTestsUsingSession.HasEntitiesWithGroups.Multi;
import com.googlecode.objectify.test.LoadFieldRefTestsUsingSession.HasEntitiesWithGroups.Single;
import com.googlecode.objectify.test.entity.Trivial;
import com.googlecode.objectify.test.util.TestBase;
import com.googlecode.objectify.test.util.TestObjectify;

/**
 * Tests the fetching system for simple parent values.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class LoadFieldRefTestsUsingSession extends TestBase
{
	Trivial t1;
	Trivial t2;
	Trivial tNone1;
	Trivial tNone2;
	Key<Trivial> k1;
	Key<Trivial> k2;
	Key<Trivial> kNone1;
	Key<Trivial> kNone2;

	/** */
	@BeforeMethod
	public void createTwo() {
		fact.register(Trivial.class);
		TestObjectify ofy = fact.begin();

		t1 = new Trivial("foo", 11);
		k1 = ofy.put(t1);

		t2 = new Trivial("bar", 22);
		k2 = ofy.put(t2);

		tNone1 = new Trivial(123L, "fooNone", 33);
		tNone2 = new Trivial(456L, "barNone", 44);

		kNone1 = Key.create(tNone1);
		kNone2 = Key.create(tNone2);
	}

	/** */
	@Entity
	public static class HasEntitiesWithGroups {
		public static class Single {}
		public static class Multi {}

		public @Id Long id;
		public @Load(Single.class) Ref<Trivial> single;
		public @Load(Multi.class) List<Ref<Trivial>> multi = new ArrayList<Ref<Trivial>>();
	}

	/** */
	@Test
	public void testGrouping() throws Exception
	{
		fact.register(HasEntitiesWithGroups.class);

		HasEntitiesWithGroups he = new HasEntitiesWithGroups();
		he.single = Ref.create(k1);
		he.multi.add(Ref.create(k1));
		he.multi.add(Ref.create(k2));
		HasEntitiesWithGroups fetched = this.putClearGet(he);

		Key<HasEntitiesWithGroups> hekey = Key.create(he);

		assert !fetched.single.isLoaded();
		assert !fetched.multi.get(0).isLoaded();
		assert !fetched.multi.get(1).isLoaded();

		assert fetched.single.equivalent(k1);
		assert fetched.single.equivalent(fetched.multi.get(0));

		TestObjectify ofy = fact.begin();

		fetched = ofy.load().group(Single.class).key(hekey).get();
		assert fetched.single.isLoaded();
		assert fetched.multi.get(0).isLoaded();
		assert !fetched.multi.get(1).isLoaded();

		assert fetched.single.get() == fetched.multi.get(0).get();

		assert fetched.single.equivalent(fetched.multi.get(0));
		assert fetched.single.equivalent(k1);
		assert fetched.single.get().getId().equals(t1.getId());
		assert fetched.single.get().getSomeString().equals(t1.getSomeString());

		fetched = ofy.load().group(Multi.class).key(hekey).get();
		assert fetched.single.isLoaded();
		assert fetched.multi.get(0).isLoaded();
		assert fetched.multi.get(1).isLoaded();

		assert fetched.single.get() == fetched.multi.get(0).get();

		assert fetched.multi.get(0).get().getId().equals(t1.getId());
		assert fetched.multi.get(0).get().getSomeString().equals(t1.getSomeString());
		assert fetched.multi.get(1).get().getId().equals(t2.getId());
		assert fetched.multi.get(1).get().getSomeString().equals(t2.getSomeString());

		fetched = ofy.load().group(Single.class).group(Multi.class).key(hekey).get();
		assert fetched.single.isLoaded();
		assert fetched.multi.get(0).isLoaded();
		assert fetched.multi.get(1).isLoaded();

		assert fetched.single.get() == fetched.multi.get(0).get();

		assert fetched.multi.get(0).get().getId().equals(t1.getId());
		assert fetched.multi.get(0).get().getSomeString().equals(t1.getSomeString());
		assert fetched.multi.get(1).get().getId().equals(t2.getId());
		assert fetched.multi.get(1).get().getSomeString().equals(t2.getSomeString());
	}
}