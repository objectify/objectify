/*
 * $Id: BeanMixin.java 1075 2009-05-07 06:41:19Z lhoriman $
 * $URL: https://subetha.googlecode.com/svn/branches/resin/rtest/src/org/subethamail/rtest/util/BeanMixin.java $
 */

package com.googlecode.objectify.test;

import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.EntityQuery;
import com.google.cloud.datastore.FullEntity;
import com.google.cloud.datastore.NullValue;
import com.google.cloud.datastore.StringValue;
import com.google.cloud.datastore.StructuredQuery;
import com.google.cloud.datastore.StructuredQuery.PropertyFilter;
import com.google.cloud.datastore.Value;
import com.google.common.collect.Lists;
import com.googlecode.objectify.test.util.TestBase;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
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
	 * Just making sure that queries work the same way they used to
	 */
	@Test
	void doesTheDatastoreDetectUnindexedProperties() throws Exception {
		final FullEntity<?> ent = makeEntity("Test")
				.set("indexed", StringValue.newBuilder("foo").setExcludeFromIndexes(false).build())
				.set("unindexed", StringValue.newBuilder("foo").setExcludeFromIndexes(true).build())
				.build();

		datastore().put(ent);

		final EntityQuery indexedQuery = StructuredQuery.newEntityQueryBuilder().setKind("Test").setFilter(PropertyFilter.eq("indexed", "foo")).build();
		final List<Entity> indexedResults = Lists.newArrayList(datastore().run(indexedQuery));
		assertThat(indexedResults).hasSize(1);

		final EntityQuery unindexedQuery = StructuredQuery.newEntityQueryBuilder().setKind("Test").setFilter(PropertyFilter.eq("unindexed", "foo")).build();
		final List<Entity> unindexedResults = Lists.newArrayList(datastore().run(unindexedQuery));
		assertThat(unindexedResults).hasSize(0);

	}


}