/*
 * $Id: BeanMixin.java 1075 2009-05-07 06:41:19Z lhoriman $
 * $URL: https://subetha.googlecode.com/svn/branches/resin/rtest/src/org/subethamail/rtest/util/BeanMixin.java $
 */

package com.googlecode.objectify.test;

import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.FullEntity;
import com.google.cloud.datastore.Value;
import com.googlecode.objectify.test.util.TestBase;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
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
class DatastoreEntityTests extends TestBase {

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	private static class Thing {
		private String name;
		private int age;
	}

	@NoArgsConstructor
	private static class SerializableThing extends Thing implements Serializable {
		public SerializableThing(final String name, final int age) {
			super(name, age);
		}
	}
	
	/**
	 * What happens when you put empty collections in an Entity?
	 */
	@Test
	void emptyCollectionPropertiesDisappear() throws Exception {
		final FullEntity<?> ent = makeEntity("Test")
				.set("empty", new ArrayList<>())
				.build();

		final Entity completed = datastore().put(ent);

		final Entity fetched = datastore().get(completed.getKey());
		
		assertThat(fetched.getNames()).doesNotContain("empty");
	}

	/**
	 * What happens when you put a single null in a collection in an Entity?
	 */
	@Test
	void collectionPropertiesCanContainNull() throws Exception {
		final FullEntity<?> ent = makeEntity("Test")
				.set("hasNull", Collections.singletonList(null))
				.build();

		final Entity completed = datastore().put(ent);

		final Entity fetched = datastore().get(completed.getKey());

		final List<Value<?>> whatIsIt = fetched.getList("hasNull");

		assertThat(whatIsIt).containsExactly((Object)null);
	}
}