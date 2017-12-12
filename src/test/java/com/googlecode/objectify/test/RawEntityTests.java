/*
 */

package com.googlecode.objectify.test;

import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.FullEntity;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.test.util.TestBase;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * Tests of using the native datastore Entity type
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
class RawEntityTests extends TestBase {

	/** */
	@Test
	void saveAndLoadRawEntityWorks() throws Exception {
		final FullEntity<?> ent = makeEntity("asdf")
				.set("foo", "bar")
				.build();

		final Key<?> key = ofy().save().entity(ent).now();
		ofy().clear();

		final Entity fetched1 = (Entity)ofy().load().key(key).now();
		ofy().clear();

		final Entity fetched2 = ofy().load().<Entity>value(fetched1).now();

		assertThat(fetched2).isEqualTo(fetched1);
	}

	/** */
	@Test
	void deleteRawEntityWorks() throws Exception {
		final FullEntity<?> ent = makeEntity("asdf")
				.set("foo", "bar")
				.build();

		final Entity saved = datastore().put(ent);
		ofy().clear();

		ofy().delete().entity(saved);

		final Entity fetched = ofy().load().<Entity>value(saved).now();
		assertThat(fetched).isNull();
	}
}