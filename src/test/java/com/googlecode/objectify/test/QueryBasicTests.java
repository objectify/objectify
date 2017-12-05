/*
 */

package com.googlecode.objectify.test;

import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Query;
import com.google.common.collect.Lists;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.test.entity.Trivial;
import com.googlecode.objectify.test.util.TestBase;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static com.googlecode.objectify.ObjectifyService.factory;
import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * Tests of basic query operations
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
class QueryBasicTests extends TestBase {

	/** */
	@Test
	void simpleQueryWorks() throws Exception {
		factory().register(Trivial.class);

		final Trivial triv = new Trivial(123L, "foo", 12);
		ofy().save().entity(triv).now();

		final Query<Entity> q = Query.newEntityQueryBuilder().setKind("Trivial").build();
		final List<Entity> stuff = Lists.newArrayList(datastore().run(q));
		assertThat(stuff).hasSize(1);

		assertThat(ofy().load().type(Trivial.class)).containsExactly(triv);
	}

	/**
	 * Doesn't actually test chunk behavior (which is really a performance consideration). Rather
	 * just makes sure that if we set a small chunk size, everything works correctly.
	 */
	@Test
	void chunking() throws Exception {
		factory().register(Trivial.class);

		final List<Trivial> trivs = new ArrayList<>(100);
		for (int i = 0; i < 100; i++) {
			final Trivial triv = new Trivial(1000L + i, "foo" + i, i);
			trivs.add(triv);
		}

		ofy().save().entities(trivs).now();

		assertThat(trivs).hasSize(100);

		int count = 0;
		for (final Trivial triv: ofy().load().type(Trivial.class).chunk(2)) {
			assertThat(triv.getSomeNumber()).isEqualTo(count);
			count++;
		}
		assertThat(count).isEqualTo(100);
	}

	/** */
	@Test
	void loadByKindWorks() throws Exception {
		factory().register(Trivial.class);

		final Trivial triv1 = new Trivial(123L, "foo1", 12);
		ofy().save().entities(triv1).now();
		ofy().clear();

		final Trivial fetched1 = ofy().load().<Trivial>kind(Key.getKind(Trivial.class)).id(triv1.getId()).now();
		assertThat(fetched1).isEqualTo(triv1);
	}

	/** */
	@Test
	void queryByKindWorks() throws Exception {
		factory().register(Trivial.class);

		final Trivial triv1 = new Trivial(123L, "foo1", 12);
		ofy().save().entities(triv1).now();
		ofy().clear();

		final List<Trivial> fetched = ofy().load().<Trivial>kind(Key.getKind(Trivial.class)).list();
		assertThat(fetched).containsExactly(triv1);
	}

	/** */
	@Test
	void queryByKindWithFilterWorks() throws Exception {
		factory().register(Trivial.class);

		final Trivial triv1 = new Trivial(123L, "foo1", 12);
		ofy().save().entities(triv1).now();
		ofy().clear();

		final List<Trivial> fetched = ofy().load().<Trivial>kind(Key.getKind(Trivial.class)).filter("someString", "foo1").list();
		assertThat(fetched).containsExactly(triv1);
	}
}