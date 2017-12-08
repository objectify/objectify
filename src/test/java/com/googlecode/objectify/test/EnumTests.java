/*
 */

package com.googlecode.objectify.test;

import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.test.util.TestBase;
import lombok.Data;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static com.googlecode.objectify.ObjectifyService.factory;
import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * Tests of Enums, including Enums in arrays and lists
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
class EnumTests extends TestBase {
	private enum Color {
		RED,
		GREEN
	}

	/** */
	@Entity
	@Cache
	@Index
	@Data
	private static class HasEnums {
		private @Id Long id;

		private Color color;
		private List<Color> colors;
		private Color[] colorsArray;
	}

	/** */
	@BeforeEach
	void setUpExtra()
	{
		factory().register(HasEnums.class);
	}

	/** */
	@Test
	void simpleEnumsArePersisted() throws Exception {
		final HasEnums he = new HasEnums();
		he.color = Color.RED;

		final HasEnums fetched = saveClearLoad(he);

		assertThat(fetched).isEqualTo(he);
	}

	/** */
	@Test
	void enumCollectionsArePersisted() throws Exception {
		final HasEnums he = new HasEnums();
		he.colors = Arrays.asList(Color.RED, Color.GREEN);

		final HasEnums fetched = saveClearLoad(he);

		assertThat(fetched).isEqualTo(he);
	}

	/** */
	@Test
	void enumArraysArePersisted() throws Exception {
		final HasEnums he = new HasEnums();
		he.colorsArray = new Color[] { Color.RED, Color.GREEN };

		final HasEnums fetched = saveClearLoad(he);

		assertThat(fetched.colorsArray).isEqualTo(he.colorsArray);
	}

	/** */
	@Test
	void canFilterByEnum() throws Exception {
		final HasEnums he = new HasEnums();
		he.color = Color.GREEN;
		ofy().save().entity(he).now();

		final HasEnums fetched = ofy().load().type(HasEnums.class).filter("color =", Color.GREEN).first().now();
		assertThat(fetched).isEqualTo(he);
	}
}