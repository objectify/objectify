/*
 */

package com.googlecode.objectify.test;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Ignore;
import com.googlecode.objectify.annotation.Load;
import com.googlecode.objectify.annotation.OnSave;
import com.googlecode.objectify.test.LoadFieldRefTestsUsingSession.HasEntitiesWithGroups.Multi;
import com.googlecode.objectify.test.LoadFieldRefTestsUsingSession.HasEntitiesWithGroups.Single;
import com.googlecode.objectify.test.entity.Trivial;
import com.googlecode.objectify.test.util.TestBase;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static com.googlecode.objectify.test.util.TestObjectifyService.fact;
import static com.googlecode.objectify.test.util.TestObjectifyService.ofy;

/**
 * Tests the fetching system for normal fields. This uses the session so we can experiment with
 * loading and re-loading with different entity groups.
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
		fact().register(Trivial.class);

		t1 = new Trivial("foo", 11);
		k1 = ofy().save().entity(t1).now();

		t2 = new Trivial("bar", 22);
		k2 = ofy().save().entity(t2).now();

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

		public @Ignore boolean onSaveFired = false;
		@OnSave void onSave() { onSaveFired = true; }
	}

	/** */
	@Test
	public void loadGroupsSpecifiedOnCachedEntitiesStillLoad() throws Exception {
		fact().register(HasEntitiesWithGroups.class);

		HasEntitiesWithGroups he = new HasEntitiesWithGroups();
		he.single = Ref.create(k1);
		he.multi.add(Ref.create(k1));
		he.multi.add(Ref.create(k2));
		HasEntitiesWithGroups fetched = ofy().saveClearLoad(he);

		Key<HasEntitiesWithGroups> hekey = Key.create(he);

		assert !fetched.single.isLoaded();
		assert !fetched.multi.get(0).isLoaded();
		assert !fetched.multi.get(1).isLoaded();

		assert fetched.single.equivalent(k1);
		assert fetched.single.equivalent(fetched.multi.get(0));

		fetched = ofy().load().group(Single.class).key(hekey).now();
		assert fetched.single.isLoaded();
		assert fetched.multi.get(0).isLoaded();
		assert !fetched.multi.get(1).isLoaded();

		assert fetched.single.get() == fetched.multi.get(0).get();

		assert fetched.single.equivalent(fetched.multi.get(0));
		assert fetched.single.equivalent(k1);
		assert fetched.single.get().getId().equals(t1.getId());
		assert fetched.single.get().getSomeString().equals(t1.getSomeString());

		fetched = ofy().load().group(Multi.class).key(hekey).now();
		assert fetched.single.isLoaded();
		assert fetched.multi.get(0).isLoaded();
		assert fetched.multi.get(1).isLoaded();

		assert fetched.single.get() == fetched.multi.get(0).get();

		assert fetched.multi.get(0).get().getId().equals(t1.getId());
		assert fetched.multi.get(0).get().getSomeString().equals(t1.getSomeString());
		assert fetched.multi.get(1).get().getId().equals(t2.getId());
		assert fetched.multi.get(1).get().getSomeString().equals(t2.getSomeString());

		fetched = ofy().load().group(Single.class).group(Multi.class).key(hekey).now();
		assert fetched.single.isLoaded();
		assert fetched.multi.get(0).isLoaded();
		assert fetched.multi.get(1).isLoaded();

		assert fetched.single.get() == fetched.multi.get(0).get();

		assert fetched.multi.get(0).get().getId().equals(t1.getId());
		assert fetched.multi.get(0).get().getSomeString().equals(t1.getSomeString());
		assert fetched.multi.get(1).get().getId().equals(t2.getId());
		assert fetched.multi.get(1).get().getSomeString().equals(t2.getSomeString());
	}

	/**
	 * Fragment of other test, just easier to debug.
	 */
	@Test
	public void loadGroupsSpecifiedOnCachedEntitiesStillLoad_simplerCase() throws Exception {
		fact().register(HasEntitiesWithGroups.class);

		HasEntitiesWithGroups he = new HasEntitiesWithGroups();
		he.single = Ref.create(k1);

		HasEntitiesWithGroups fetched = ofy().saveClearLoad(he);
		assert !fetched.single.isLoaded();

		fetched = ofy().load().group(Single.class).entity(he).now();
		assert fetched.single.isLoaded();

		// The code internally performs a save to a throwaway Entity, but we want to make sure that doesn't have
		// any public effects.
		assert !fetched.onSaveFired;
	}
}