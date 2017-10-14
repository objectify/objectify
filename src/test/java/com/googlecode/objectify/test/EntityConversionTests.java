/*
 */

package com.googlecode.objectify.test;

import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.test.entity.Trivial;
import com.googlecode.objectify.test.util.TestBase;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static com.googlecode.objectify.ObjectifyService.factory;
import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * Tests of basic entity manipulation.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class EntityTests extends TestBase {

	/**
	 * A fruit.
	 *
	 * @author Scott Hernandez
	 */
	@Entity
	@Cache
	@NoArgsConstructor
	@Data
	private static abstract class Fruit {
		@Id
		private Long id;
		private String color;
		private String taste;

		protected Fruit(String color, String taste) {
			this.color = color;
			this.taste = taste;
		}
	}

	/**
	 * A fruit, an apple.
	 *
	 * @author Scott Hernandez
	 */
	@Entity
	@Cache
	@Data
	@EqualsAndHashCode(callSuper = true)
	@NoArgsConstructor
	private static class Apple extends Fruit {
		public static final String COLOR = "red";
		public static final String TASTE = "sweet";

		private String size;

		public Apple(String color, String taste) {
			super(color, taste);
			this.size = "small";
		}
	}

	/** */
	@Test
	void testApple() throws Exception {
		factory().register(Apple.class);

		final Apple apple = new Apple(Apple.COLOR, Apple.TASTE);
		final Apple fetched = saveClearLoad(apple);
		assertThat(fetched).isEqualTo(apple);
	}

	/** */
	@Test
	void convertsToPojoAndBack() throws Exception {
		factory().register(Trivial.class);

		final Trivial triv = new Trivial(123L, "blah", 456);

		final com.google.appengine.api.datastore.Entity ent = ofy().save().toEntity(triv);
		assertThat(ent.getKey().getId()).isEqualTo(123L);
		assertThat(ent.getProperty("someString")).isEqualTo("blah");
		assertThat(ent.getProperty("someNumber")).isEqualTo(456L);

		final Trivial converted = ofy().load().fromEntity(ent);
		assertThat(converted).isEqualTo(triv);
	}

}