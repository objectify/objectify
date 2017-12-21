/*
 */

package com.googlecode.objectify.test;

import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Query;
import com.google.common.collect.Lists;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.test.entity.Trivial;
import com.googlecode.objectify.test.util.LocalDatastoreExtensionEventual;
import com.googlecode.objectify.test.util.LocalMemcacheExtension;
import com.googlecode.objectify.test.util.MockitoExtension;
import com.googlecode.objectify.test.util.ObjectifyExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.google.common.truth.Truth.assertThat;
import static com.googlecode.objectify.ObjectifyService.factory;
import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * Tests of queries when they are eventual. Does not extend TestBase! Uses a different GAEExtension.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
@ExtendWith({
		MockitoExtension.class,
		LocalDatastoreExtensionEventual.class,
		LocalMemcacheExtension.class,
		ObjectifyExtension.class,
})
class QueryEventualityTests {

	/** */
	private Trivial triv1;
	private Trivial triv2;
	private List<Key<Trivial>> keys;

	/** */
	@BeforeEach
	void setUpExtra() {
		factory().register(Trivial.class);

		this.triv1 = new Trivial("foo1", 1);
		this.triv2 = new Trivial("foo2", 2);

		final Map<Key<Trivial>, Trivial> result = ofy().save().entities(triv1, triv2).now();

		this.keys = new ArrayList<>(result.keySet());

		// This should apply the writes
		final Query<Entity> q = Query.newEntityQueryBuilder().setKind("Trivial").build();
		Lists.newArrayList(factory().datastore().run(q));

		// For some reason this doesn't.
		ofy().load().keys(keys).size();
	}

	/**
	 * Delete creates a negative cache result, so when the value comes back we should not insert a null
	 * but rather pretend the value does not exist.
	 */
	@Test
	void deleteWorks() throws Exception {
		// Should be an unapplied write
		ofy().delete().entity(triv1).now();

		final List<Trivial> found = ofy().load().type(Trivial.class).list();
		assertThat(found).containsExactly(triv2);
	}

	/**
	 * Delete creates a negative cache result, so when the value comes back we should not insert a null
	 * but rather pretend the value does not exist.
	 */
	@Test
	void deleteAllWorks() throws Exception {
		ofy().delete().entities(triv1, triv2).now();

		List<Trivial> found = ofy().load().type(Trivial.class).list();
		assertThat(found).isEmpty();
	}
}
