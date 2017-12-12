/*
 */

package com.googlecode.objectify.test;

import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.EntityQuery;
import com.google.cloud.datastore.EntityValue;
import com.google.cloud.datastore.FullEntity;
import com.google.cloud.datastore.IncompleteKey;
import com.google.cloud.datastore.ListValue;
import com.google.cloud.datastore.LongValue;
import com.google.cloud.datastore.NullValue;
import com.google.cloud.datastore.QueryResults;
import com.google.cloud.datastore.StructuredQuery;
import com.google.cloud.datastore.StructuredQuery.PropertyFilter;
import com.google.cloud.datastore.Value;
import com.google.common.collect.Lists;
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

	/**
	 * This isn't currently documented, so figure it out through trial and error
	 */
	@Test
	void intermediateEmbeddedEntitiesAlwaysNeedToBeIndexed() throws Exception {
		final FullEntity<IncompleteKey> thing = FullEntity.newBuilder().set("foo", "bar").build();

		final FullEntity<?> ent = makeEntity("Test")
				.set("thing", EntityValue.newBuilder(thing).setExcludeFromIndexes(true).build())
				.build();

		final Entity completed = datastore().put(ent);

		final EntityQuery query = StructuredQuery.newEntityQueryBuilder()
				.setKind("Test")
				.setFilter(PropertyFilter.eq("thing.foo", "bar"))
				.build();

		final List<Entity> fetched = Lists.newArrayList(datastore().run(query));

		// If we don't exclude, this works
		//assertThat(fetched).containsExactly(completed);
		// Because we excluded "thing", everything below is not indexed
		assertThat(fetched).isEmpty();
	}

	/**
	 * Weird, it looks like cursors restart if you grab them at the end of iteration?
	 */
	@Test
	void cursorBehaviorAtTheEndOfIteration() throws Exception {
		final FullEntity<?> ent = makeEntity("Test").build();
		datastore().put(ent, ent);	// make two

		final QueryResults<Entity> from0 = datastore().run(
				StructuredQuery.newEntityQueryBuilder()
						.setKind("Test")
						.build());
		final Entity ent1 = from0.next();
		final Entity ent2 = from0.next();

		assertThat(from0.hasNext()).isFalse();

		final QueryResults<Entity> from2 = datastore().run(
				StructuredQuery.newEntityQueryBuilder()
						.setStartCursor(from0.getCursorAfter())
						.build());
		assertThat(from2.hasNext()).isFalse();

		final QueryResults<Entity> from2Again = datastore().run(
				StructuredQuery.newEntityQueryBuilder()
						.setStartCursor(from2.getCursorAfter())
						.build());
		assertThat(from2Again.hasNext()).isFalse();
	}

}