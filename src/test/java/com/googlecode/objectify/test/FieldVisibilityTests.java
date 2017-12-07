/*
 */

package com.googlecode.objectify.test;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.Unindex;
import com.googlecode.objectify.test.util.TestBase;
import lombok.Data;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.google.common.truth.Truth.assertThat;
import static com.googlecode.objectify.ObjectifyService.factory;
import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * Testing of field visiblity (package/private/etc)
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
class FieldVisibilityTests extends TestBase {
	/** */
	@Entity
	@Unindex
	@Data
	private static class ThingWithPrivates {
		@Id
		private Long id;

		@Index
		private Set<String> stuff = new HashSet<>();
	}

	/** */
	@Test
	void canPersistPrivateFields() throws Exception {
		factory().register(ThingWithPrivates.class);

		final ThingWithPrivates thing = new ThingWithPrivates();
		thing.stuff.add("foo");

		final ThingWithPrivates fetched = saveClearLoad(thing);

		assertThat(fetched).isEqualTo(thing);
	}

	/** */
	@Test
	void canQueryForPrivateFields() throws Exception {
		factory().register(ThingWithPrivates.class);

		final ThingWithPrivates thing = new ThingWithPrivates();
		thing.stuff.add("foo");

		ofy().save().entity(thing).now();
		ofy().clear();
		final List<ThingWithPrivates> fetched = ofy().load().type(ThingWithPrivates.class).filter("stuff", "foo").list();

		assertThat(fetched).containsExactly(thing);
	}
}