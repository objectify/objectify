/*
 */

package com.googlecode.objectify.test;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.test.entity.Child;
import com.googlecode.objectify.test.entity.Trivial;
import com.googlecode.objectify.test.util.TestBase;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static com.googlecode.objectify.ObjectifyService.factory;
import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * Tests of simple key allocations
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
class AllocateTests extends TestBase {

	/** */
	@Test
	void canAllocateIdsInARange() throws Exception {
		factory().register(Trivial.class);

		final List<Key<Trivial>> range = factory().allocateIds(Trivial.class, 5);

		final List<Key<Trivial>> keys = Lists.newArrayList(range.iterator());
		assertThat(keys).hasSize(5);
		assertThat(Sets.newHashSet(keys)).containsExactlyElementsIn(range);

		// Create an id with a put and verify it is not one of the previous
		final Trivial triv = new Trivial("foo", 3);
		final Key<Trivial> key = ofy().save().entity(triv).now();

		assertThat(key).isNotIn(range);
	}

	/** */
	@Test
	void canAllocateAnIdRangeWithAParent() throws Exception {
		factory().register(Trivial.class);
		factory().register(Child.class);

		final Key<Trivial> parentKey = Key.create(Trivial.class, 123);
		final List<Key<Child>> range = factory().allocateIds(parentKey, Child.class, 5);

		final List<Key<Child>> keys = Lists.newArrayList(range.iterator());
		assertThat(keys).hasSize(5);
		assertThat(Sets.newHashSet(keys)).containsExactlyElementsIn(range);

		// Create an id with a put and verify it is not one of the previous
		final Child ch = new Child(parentKey, "foo");
		final Key<Child> key = ofy().save().entity(ch).now();

		assertThat(key).isNotIn(range);
	}
}