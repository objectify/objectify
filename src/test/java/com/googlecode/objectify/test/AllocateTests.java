/*
 */

package com.googlecode.objectify.test;

import com.google.common.collect.Lists;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.KeyRange;
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

		final KeyRange<Trivial> range = factory().allocateIds(Trivial.class, 5);

		final List<Key<Trivial>> keys = Lists.newArrayList(range.iterator());
		assertThat(keys).hasSize(5);

		long previousId = 0;
		for (final Key<Trivial> key : keys) {
			assertThat(key.getId()).isGreaterThan(previousId);
			previousId = key.getId();
		}

		// Create an id with a put and verify it is > than the last
		final Trivial triv = new Trivial("foo", 3);
		ofy().save().entity(triv).now();

		assertThat(triv.getId()).isGreaterThan(previousId);
	}

	/** */
	@Test
	void canAllocateAnIdRangeWithAParent() throws Exception {
		factory().register(Trivial.class);
		factory().register(Child.class);

		final Key<Trivial> parentKey = Key.create(Trivial.class, 123);
		final KeyRange<Child> range = factory().allocateIds(parentKey, Child.class, 5);

		final List<Key<Child>> keys = Lists.newArrayList(range.iterator());
		assertThat(keys).hasSize(5);

		long previousId = 0;
		for (final Key<Child> key : keys) {
			assertThat(key.getId()).isGreaterThan(previousId);
			previousId = key.getId();
		}

		// Create an id with a put and verify it is > than the last
		final Child ch = new Child(parentKey, "foo");
		ofy().save().entity(ch).now();

		assertThat(ch.getId()).isGreaterThan(previousId);
	}
}