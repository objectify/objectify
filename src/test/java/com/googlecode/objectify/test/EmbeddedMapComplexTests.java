package com.googlecode.objectify.test;

import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.test.util.TestBase;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static com.google.common.truth.Truth.assertThat;
import static com.googlecode.objectify.ObjectifyService.factory;

/**
 * Test persisting of Map with an embedded value.
 */
class EmbeddedMapComplexTests extends TestBase {

	@Data
	private static class Thing {
		private String name;
		private Long weight;
	}

	@com.googlecode.objectify.annotation.Entity
	@Data
	@EqualsAndHashCode(exclude = "id")
	private static class HasMapEmbed {
		@Id Long id;
		Thing thing;
		Map<String, Thing> things = new HashMap<>();
	}


	@com.googlecode.objectify.annotation.Entity
	private static class HasNestedMapEmbed {
		@Id Long id;
		Map<String, HasMapEmbed> nestedThings = new HashMap<>();
	}

	/** Need to be able to create these easily */
	private HasMapEmbed createHasMapEmbed(int suffix) {
		final HasMapEmbed hasMap = new HasMapEmbed();

		hasMap.thing = new Thing();
		hasMap.thing.name = "Chair" + suffix;
		hasMap.thing.weight = 100L + suffix;

		final Thing fishThing = new Thing();
		fishThing.name = "Blue whale" + suffix;
		fishThing.weight = 1000000L + suffix;
		hasMap.things.put("fish" + suffix, fishThing);

		final Thing fruitThing = new Thing();
		fruitThing.name = "Apple" + suffix;
		fruitThing.weight = 0L + suffix;
		hasMap.things.put("fruit" + suffix, fruitThing);

		return hasMap;
	}

	@Test
	void embeddedMapsArePersisted() throws Exception {
		factory().register(HasMapEmbed.class);

		final HasMapEmbed hasMap = createHasMapEmbed(1);
		final HasMapEmbed fetched = saveClearLoad(hasMap);

		assertThat(fetched).isEqualTo(hasMap);
	}

	@Test
	void nestedEmbeddedMapsArePersisted() throws Exception {
		factory().register(HasNestedMapEmbed.class);

		final HasNestedMapEmbed hasNested = new HasNestedMapEmbed();

		final HasMapEmbed hasMap1 = createHasMapEmbed(1);
		hasMap1.id = 123L;
		final HasMapEmbed hasMap2 = createHasMapEmbed(2);
		hasMap2.id = 456L;

		hasNested.nestedThings.put("one", hasMap1);
		hasNested.nestedThings.put("two", hasMap2);

		final HasNestedMapEmbed fetched = saveClearLoad(hasNested);

		assertThat(fetched.nestedThings).isEqualTo(hasNested.nestedThings);
	}
}
