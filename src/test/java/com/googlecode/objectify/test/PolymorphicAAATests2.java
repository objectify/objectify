/*
 */

package com.googlecode.objectify.test;

import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.test.util.TestBase;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static com.googlecode.objectify.ObjectifyService.factory;

/**
 * This test seems superfluous
 */
class PolymorphicAAATests2 extends TestBase {

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
		Long id;
		String color;
		String taste;

		Fruit(String color, String taste) {
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

		String size;

		Apple(String color, String taste) {
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
}