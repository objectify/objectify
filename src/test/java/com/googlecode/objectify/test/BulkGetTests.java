package com.googlecode.objectify.test;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.test.entity.Trivial;
import com.googlecode.objectify.test.util.TestBase;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Map;

import static com.google.common.truth.Truth.assertThat;
import static com.googlecode.objectify.ObjectifyService.factory;
import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * Tests that bulk gets return results having the same order as the argument
 */
class BulkGetTests extends TestBase {
	/** */
	@Test
	void bulkGetPreservesOrder() {
		factory().register(Trivial.class);

		final Trivial t1 = new Trivial("foo", 5);
		final Trivial t2 = new Trivial("bar", 6);

		final Key<Trivial> k1 = ofy().save().entity(t1).now();
		final Key<Trivial> k2 = ofy().save().entity(t2).now();

		// get k1, then k2
		final Map<Key<Trivial>, Trivial> map = ofy().load().keys(Arrays.asList(k1, k2));
		assertThat(map.keySet()).containsExactly(k1, k2).inOrder();

		// get k2, then k1
		final Map<Key<Trivial>, Trivial> map2 = ofy().load().keys(Arrays.asList(k2, k1));
		assertThat(map2.keySet()).containsExactly(k2, k1).inOrder();
	}
}
