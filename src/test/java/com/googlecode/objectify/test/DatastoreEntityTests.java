/*
 * $Id: BeanMixin.java 1075 2009-05-07 06:41:19Z lhoriman $
 * $URL: https://subetha.googlecode.com/svn/branches/resin/rtest/src/org/subethamail/rtest/util/BeanMixin.java $
 */

package com.googlecode.objectify.test;

import com.google.appengine.api.datastore.Entity;
import com.googlecode.objectify.test.util.TestBase;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
	 * What happens when you put an object in an Entity?
	 */
	@Test
	void propertiesCannotBeArbitraryObjects() throws Exception {
		final Thing thing = new Thing("foo", 10);

		final Entity ent = new Entity("Test");
		assertThrows(IllegalArgumentException.class, () -> ent.setProperty("thing", thing));
	}

	/**
	 * What happens if it is serializable?
	 */
	@Test
	void propertiesCannotBeArbitraryObjectsNotEvenIfSerializable() throws Exception {
		final SerializableThing thing = new SerializableThing("foo", 10);

		final Entity ent = new Entity("Test");
		assertThrows(IllegalArgumentException.class, () -> ent.setProperty("thing", thing));
	}

	/**
	 * What happens when you put empty collections in an Entity?
	 */
	@Test
	void emptyCollectionPropertiesDisappear() throws Exception {
		final Entity ent = new Entity("Test");
		ent.setProperty("empty", new ArrayList<>());

		ds().put(ent);
		
		final Entity fetched = ds().get(ent.getKey());
		
		Object whatIsIt = fetched.getProperty("empty");
		assertThat(whatIsIt).isNull();
	}

	/**
	 * What happens when you put a single null in a collection in an Entity?
	 */
	@Test
	void collectionPropertiesCanContainNull() throws Exception {
		final Entity ent = new Entity("Test");
		ent.setProperty("hasNull", Collections.singletonList(null));

		ds().put(ent);
		
		final Entity fetched = ds().get(ent.getKey());
		
		@SuppressWarnings("unchecked")
		Collection<Object> whatIsIt = (Collection<Object>)fetched.getProperty("hasNull");

		assertThat(whatIsIt).containsExactly((Object)null);
	}
}