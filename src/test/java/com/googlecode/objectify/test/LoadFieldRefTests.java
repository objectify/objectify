/*
 */

package com.googlecode.objectify.test;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Load;
import com.googlecode.objectify.test.entity.Trivial;
import com.googlecode.objectify.test.util.TestBase;
import lombok.Data;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static com.googlecode.objectify.ObjectifyService.factory;
import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * Tests the fetching system for simple parent values.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
class LoadFieldRefTests extends TestBase {
	private Trivial t1;
	private Trivial t2;
	private Trivial tNone1;
	private Trivial tNone2;
	private Key<Trivial> k1;
	private Key<Trivial> k2;
	private Key<Trivial> kNone1;
	private Key<Trivial> kNone2;

	/** */
	@BeforeEach
	void createTwo() {
		factory().register(Trivial.class);

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
	@Data
	private static class HasEntities {
		@Id Long id;
		@Load Ref<Trivial> single;
		@Load List<Ref<Trivial>> multi = new ArrayList<>();
	}

	/** */
	@Test
	void testTargetsExist() throws Exception {
		factory().register(HasEntities.class);

		final HasEntities he = new HasEntities();
		he.single = Ref.create(k1);
		he.multi.add(Ref.create(k1));
		he.multi.add(Ref.create(k2));

		final HasEntities fetched = saveClearLoad(he);

		assertThat(fetched.single.isLoaded()).isTrue();
		assertThat(fetched.single.get()).isEqualTo(t1);

		for (final Ref<?> ref: fetched.multi)
			assertThat(ref.isLoaded()).isTrue();

		assertThat(fetched.multi.get(0).get()).isSameInstanceAs(fetched.single.get());
		assertThat(fetched.multi.get(1).get()).isEqualTo(t2);
	}

	/** */
	@Test
	void testTargetsDontExist() throws Exception {
		factory().register(HasEntities.class);

		final HasEntities he = new HasEntities();
		he.single = Ref.create(kNone1);
		he.multi.add(Ref.create(kNone1));
		he.multi.add(Ref.create(kNone2));
		final HasEntities fetched = saveClearLoad(he);

		assertThat(fetched.single.isLoaded()).isTrue();
		assertThat(fetched.single.get()).isNull();

		for (final Ref<?> ref: fetched.multi)
			assertThat(ref.isLoaded()).isTrue();

		assertThat(fetched.multi.get(0).get()).isNull();
		assertThat(fetched.multi.get(1).get()).isNull();
	}

	/** */
	@Entity
	@Data
	private static class ListNode {
		@Id Long id;
		@Load Ref<ListNode> next;
		String foo;
	}

	/** */
	@Test
	void testTwoLevelsOfFetch() throws Exception {
		factory().register(ListNode.class);

		final ListNode node3 = new ListNode();
		node3.foo = "foo3";
		ofy().save().entity(node3).now();

		final ListNode node2 = new ListNode();
		node2.foo = "foo2";
		node2.next = Ref.create(node3);
		ofy().save().entity(node2).now();

		final ListNode node1 = new ListNode();
		node1.foo = "foo1";
		node1.next = Ref.create(node2);
		ofy().save().entity(node1).now();

		ofy().clear();
		final ListNode fetched = ofy().load().entity(node1).now();

		assertThat(fetched.foo).isEqualTo(node1.foo);
		assertThat(fetched.next.isLoaded()).isTrue();
		assertThat(fetched.next.get()).isEqualTo(node2);
		assertThat(fetched.next.get().next.isLoaded()).isTrue();
		assertThat(fetched.next.get().next.get()).isEqualTo(node3);
		assertThat(fetched.next.get().next.get().next).isNull();
	}

	/** */
	@Test
	void testMissingTail() throws Exception {
		factory().register(ListNode.class);

		// Node2 should not exist but should have a concrete id for node1
		final ListNode node2 = new ListNode();
		node2.id = 999L;

		final ListNode node1 = new ListNode();
		node1.foo = "foo1";
		node1.next = Ref.create(Key.create(node2));
		ofy().save().entity(node1).now();

		ofy().clear();
		final ListNode fetched = ofy().load().entity(node1).now();

		assertThat(fetched.foo).isEqualTo(node1.foo);
		assertThat(fetched.next.get()).isNull();	// it was fetched, so this should be initialized and null.
	}

	/** */
	@Entity
	private static class HasEntitiesWithGroups {
		static class Single {}
		static class Multi {}

		@Id Long id;
		@Load(Single.class) Ref<Trivial> single;
		@Load(Multi.class) List<Ref<Trivial>> multi = new ArrayList<>();
	}

	/** */
	@Test
	void testGrouping() throws Exception {
		factory().register(HasEntitiesWithGroups.class);

		final HasEntitiesWithGroups he = new HasEntitiesWithGroups();
		he.single = Ref.create(k1);
		he.multi.add(Ref.create(k1));
		he.multi.add(Ref.create(k2));
		HasEntitiesWithGroups fetched = saveClearLoad(he);

		final Key<HasEntitiesWithGroups> hekey = Key.create(he);

		assertThat(fetched.single.isLoaded()).isFalse();
		assertThat(fetched.multi.get(0).isLoaded()).isFalse();
		assertThat(fetched.multi.get(1).isLoaded()).isFalse();

		assertThat(fetched.single.equivalent(k1)).isTrue();
		assertThat(fetched.single.equivalent(fetched.multi.get(0))).isTrue();

		ofy().clear();
		fetched = ofy().load().group(HasEntitiesWithGroups.Single.class).key(hekey).now();
		assertThat(fetched.single.isLoaded()).isTrue();
		assertThat(fetched.multi.get(0).isLoaded()).isTrue();
		assertThat(fetched.multi.get(1).isLoaded()).isFalse();

		assertThat(fetched.single.equivalent(fetched.multi.get(0))).isTrue();
		assertThat(fetched.single.get()).isSameInstanceAs(fetched.multi.get(0).get());

		assertThat(fetched.single.get()).isEqualTo(t1);

		ofy().clear();
		fetched = ofy().load().group(HasEntitiesWithGroups.Multi.class).key(hekey).now();
		assertThat(fetched.single.isLoaded()).isTrue();
		assertThat(fetched.multi.get(0).isLoaded()).isTrue();
		assertThat(fetched.multi.get(1).isLoaded()).isTrue();

		assertThat(fetched.multi.get(0).equivalent(fetched.single)).isTrue();
		assertThat(fetched.single.get()).isSameInstanceAs(fetched.multi.get(0).get());

		assertThat(fetched.multi.get(0).get()).isEqualTo(t1);
		assertThat(fetched.multi.get(1).get()).isEqualTo(t2);

		ofy().clear();
		fetched = ofy().load().group(HasEntitiesWithGroups.Single.class).group(HasEntitiesWithGroups.Multi.class).key(hekey).now();
		assertThat(fetched.single.isLoaded()).isTrue();
		assertThat(fetched.multi.get(0).isLoaded()).isTrue();
		assertThat(fetched.multi.get(1).isLoaded()).isTrue();

		assertThat(fetched.multi.get(0).get()).isEqualTo(t1);
		assertThat(fetched.multi.get(1).get()).isEqualTo(t2);
		assertThat(fetched.single.get()).isSameInstanceAs(fetched.multi.get(0).get());
	}
}