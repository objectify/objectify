/*
 */

package com.googlecode.objectify.test;

import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.FullEntity;
import com.google.cloud.datastore.ListValue;
import com.google.cloud.datastore.LongValue;
import com.google.cloud.datastore.NullValue;
import com.google.cloud.datastore.Value;
import com.googlecode.objectify.test.util.TestBase;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;

/**
 * This is a set of tests that clarify exactly what happens when you put different
 * kinds of entities into the datastore.  They aren't really tests of Objectify,
 * they just help us understand the underlying behavior.
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
class DatastoreTests extends TestBase {

	/**
	 * What happens when you put empty collections in an Entity?
	 * They used to disappear. They don't anymore in the Cloud SDK.
	 */
	@Test
	void whatHappensToEmptyCollectionProperties() throws Exception {
		final FullEntity<?> ent = makeEntity("Test")
				.set("empty", new ArrayList<>())
				.build();

		final Entity completed = datastore().put(ent);

		final Entity fetched = datastore().get(completed.getKey());
		
		//assertThat(fetched.getNames()).doesNotContain("empty");
		assertThat(fetched.getList("empty")).isEmpty();
	}

	/**
	 * What happens when you put a single null in a collection in an Entity?
	 */
	@Test
	void collectionPropertiesCanContainNull() throws Exception {
		final FullEntity<?> ent = makeEntity("Test")
				.set("hasNull", Collections.singletonList(NullValue.of()))
				.build();

		final Entity completed = datastore().put(ent);

		final Entity fetched = datastore().get(completed.getKey());

		final List<Value<?>> whatIsIt = fetched.getList("hasNull");

		assertThat(whatIsIt).containsExactly(NullValue.of());
	}

	/**
	 * Weird, the datastore reorders nonindexed things ahead of indexed things in a list
	 */
	@Test
	void datastoreReordersLists() throws Exception {
		final LongValue longValue1 = LongValue.newBuilder(123L).setExcludeFromIndexes(true).build();
		final LongValue longValue2 = LongValue.newBuilder(456L).setExcludeFromIndexes(false).build();

		final List<Value<?>> list = Arrays.asList(longValue1, longValue2);
		final ListValue listValue = ListValue.of(list);

		final FullEntity<?> ent = makeEntity("Test")
				.set("list", listValue)
				.build();

		final Entity completed = datastore().put(ent);
		final Entity fetched = datastore().get(completed.getKey());

		final List<Value<?>> whatIsIt = fetched.getList("list");

		// WTF???
		assertThat(whatIsIt).containsExactly(longValue2, longValue1).inOrder();
	}
}