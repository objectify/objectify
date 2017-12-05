/*
 */

package com.googlecode.objectify.test;

import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.FullEntity;
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

		ofy().save().entity(ent).now();
		ofy().clear();

		final Entity fetched = ofy().load().<Entity>value(ent).now();

		assertThat(fetched).isEqualTo(ent);
	}

	/** */
	@Test
	void deleteRawEntityWorks() throws Exception {
		final FullEntity<?> ent = makeEntity("asdf")
				.set("foo", "bar")
				.build();

		ofy().save().entity(ent).now();
		ofy().clear();

		ofy().delete().entity(ent);

		final Entity fetched = ofy().load().<Entity>value(ent).now();
		assertThat(fetched).isNull();
	}
}