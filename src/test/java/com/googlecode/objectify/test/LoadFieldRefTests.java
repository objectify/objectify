/*
 */

package com.googlecode.objectify.test;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Load;
import com.googlecode.objectify.test.LoadFieldRefTests.HasEntitiesWithGroups.Multi;
import com.googlecode.objectify.test.LoadFieldRefTests.HasEntitiesWithGroups.Single;
import com.googlecode.objectify.test.entity.Trivial;
import com.googlecode.objectify.test.util.TestBase;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static com.googlecode.objectify.test.util.TestObjectifyService.fact;
import static com.googlecode.objectify.test.util.TestObjectifyService.ofy;

/**
 * Tests the fetching system for simple parent values.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class LoadFieldRefTests extends TestBase
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
	public static class HasEntities {
		public @Id Long id;
		public @Load Ref<Trivial> single;
		public @Load List<Ref<Trivial>> multi = new ArrayList<>();
	}

	/** */
	@Test
	public void testTargetsExist() throws Exception {
		fact().register(HasEntities.class);

		HasEntities he = new HasEntities();
		he.single = Ref.create(k1);
		he.multi.add(Ref.create(k1));
		he.multi.add(Ref.create(k2));

		HasEntities fetched = ofy().saveClearLoad(he);

		assert fetched.single.isLoaded();
		assert fetched.single.get().getId().equals(t1.getId());
		assert fetched.single.get().getSomeString().equals(t1.getSomeString());

		for (Ref<?> ref: fetched.multi)
			assert ref.isLoaded();

		assert fetched.multi.get(0).get() == fetched.single.get();

		assert fetched.multi.get(1).get().getId().equals(t2.getId());
		assert fetched.multi.get(1).get().getSomeString().equals(t2.getSomeString());
	}

	/** */
	@Test
	public void testTargetsDontExist() throws Exception {
		fact().register(HasEntities.class);

		HasEntities he = new HasEntities();
		he.single = Ref.create(kNone1);
		he.multi.add(Ref.create(kNone1));
		he.multi.add(Ref.create(kNone2));
		HasEntities fetched = ofy().saveClearLoad(he);

		assert fetched.single.isLoaded();
		assert fetched.single.get() == null;

		for (Ref<?> ref: fetched.multi)
			assert ref.isLoaded();

		assert fetched.multi.get(0).get() == null;
		assert fetched.multi.get(1).get() == null;
	}

	/** */
	@Entity
	public static class ListNode {
		public @Id Long id;
		public @Load Ref<ListNode> next;
		public String foo;
	}

	/** */
	@Test
	public void testTwoLevelsOfFetch() throws Exception {
		fact().register(ListNode.class);

		ListNode node3 = new ListNode();
		node3.foo = "foo3";
		ofy().save().entity(node3).now();

		ListNode node2 = new ListNode();
		node2.foo = "foo2";
		node2.next = Ref.create(node3);
		ofy().save().entity(node2).now();

		ListNode node1 = new ListNode();
		node1.foo = "foo1";
		node1.next = Ref.create(node2);
		ofy().save().entity(node1).now();

		ofy().clear();
		ListNode fetched = ofy().load().entity(node1).now();

		assert fetched.foo.equals(node1.foo);
		assert fetched.next.isLoaded();
		assert fetched.next.get().id.equals(node2.id);
		assert fetched.next.get().foo.equals(node2.foo);
		assert fetched.next.get().next.isLoaded();
		assert fetched.next.get().next.get().id.equals(node3.id);
		assert fetched.next.get().next.get().foo.equals(node3.foo);
		assert fetched.next.get().next.get().next == null;
	}

	/** */
	@Test
	public void testMissingTail() throws Exception {
		fact().register(ListNode.class);

		// Node2 should not exist but should have a concrete id for node1
		ListNode node2 = new ListNode();
		node2.id = 999L;

		ListNode node1 = new ListNode();
		node1.foo = "foo1";
		node1.next = Ref.create(Key.create(node2));
		ofy().save().entity(node1).now();

		ofy().clear();
		ListNode fetched = ofy().load().entity(node1).now();

		assert fetched.foo.equals(node1.foo);
		assert fetched.next.get() == null;	// it was fetched, so this should be initialized and null.
	}

	/** */
	@Entity
	public static class HasEntitiesWithGroups {
		public static class Single {}
		public static class Multi {}

		public @Id Long id;
		public @Load(Single.class) Ref<Trivial> single;
		public @Load(Multi.class) List<Ref<Trivial>> multi = new ArrayList<>();
	}

	/** */
	@Test
	public void testGrouping() throws Exception {
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

		ofy().clear();
		fetched = ofy().load().group(Single.class).key(hekey).now();
		assert fetched.single.isLoaded();
		assert fetched.multi.get(0).isLoaded();
		assert !fetched.multi.get(1).isLoaded();

		assert fetched.single.equivalent(fetched.multi.get(0));
		assert fetched.single.get() == fetched.multi.get(0).get();

		assert fetched.single.get().getId().equals(t1.getId());
		assert fetched.single.get().getSomeString().equals(t1.getSomeString());

		ofy().clear();
		fetched = ofy().load().group(Multi.class).key(hekey).now();
		assert fetched.single.isLoaded();
		assert fetched.multi.get(0).isLoaded();
		assert fetched.multi.get(1).isLoaded();

		assert fetched.multi.get(0).equivalent(fetched.single);
		assert fetched.single.get() == fetched.multi.get(0).get();

		assert fetched.multi.get(0).get().getId().equals(t1.getId());
		assert fetched.multi.get(0).get().getSomeString().equals(t1.getSomeString());
		assert fetched.multi.get(1).get().getId().equals(t2.getId());
		assert fetched.multi.get(1).get().getSomeString().equals(t2.getSomeString());

		ofy().clear();
		fetched = ofy().load().group(Single.class).group(Multi.class).key(hekey).now();
		assert fetched.single.isLoaded();
		assert fetched.multi.get(0).isLoaded();
		assert fetched.multi.get(1).isLoaded();

		assert fetched.multi.get(0).get().getId().equals(t1.getId());
		assert fetched.multi.get(0).get().getSomeString().equals(t1.getSomeString());
		assert fetched.multi.get(1).get().getId().equals(t2.getId());
		assert fetched.multi.get(1).get().getSomeString().equals(t2.getSomeString());
		assert fetched.single.get() == fetched.multi.get(0).get();
	}
}
