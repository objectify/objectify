package com.googlecode.objectify.test;

import java.util.HashMap;
import java.util.Map;

import org.testng.annotations.Test;

import com.googlecode.objectify.annotation.Embed;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.test.util.TestBase;
import com.googlecode.objectify.util.LangUtils;

/**
 * Test persisting of Map with an @Embed value.
 */
public class MapEmbedTests extends TestBase
{
	public static class Thing {
		String name;
		Long weight;

		/** Simplistic implementation */
		@Override
		public boolean equals(Object other) {
			return name.equals(((Thing)other).name) && weight.equals(((Thing)other).weight);
		}
		
		@Override
		public String toString() {
			return "Thing(name=" + name + ", weight=" + weight + ")";
		}
	}
	
	@com.googlecode.objectify.annotation.Entity
	public static class HasMapEmbed {
		@Id Long id;
		@Embed Thing thing;
		@Embed Map<String, Thing> things = new HashMap<String, Thing>();
		
		/** Simplistic implementation, ignores id */
		@Override
		public boolean equals(Object obj) {
			HasMapEmbed other = (HasMapEmbed)obj; 
			return LangUtils.objectsEqual(thing, other.thing) 
					&& LangUtils.objectsEqual(things, other.things);
		}
	}


	@com.googlecode.objectify.annotation.Entity
	public static class HasNestedMapEmbed {
		@Id Long id;
		@Embed Map<String, HasMapEmbed> nestedThings = new HashMap<String, HasMapEmbed>();
	}

	/** Need to be able to create these easily */
	private HasMapEmbed createHasMapEmbed(int suffix) {
		HasMapEmbed hasMap = new HasMapEmbed();
		
		hasMap.thing = new Thing();
		hasMap.thing.name = "Chair" + suffix;
		hasMap.thing.weight = 100L + suffix;
		
		Thing fishThing = new Thing();
		fishThing.name = "Blue whale" + suffix;
		fishThing.weight = 1000000L + suffix;
		hasMap.things.put("fish" + suffix, fishThing);
		
		Thing fruitThing = new Thing();
		fruitThing.name = "Apple" + suffix;
		fruitThing.weight = 0L + suffix;
		hasMap.things.put("fruit" + suffix, fruitThing);
		
		return hasMap;
	}

	@Test
	public void testEmbeddedMap() throws Exception {
		this.fact.register(HasMapEmbed.class);

		HasMapEmbed hasMap = createHasMapEmbed(1);
		HasMapEmbed fetched = this.putClearGet(hasMap);
		
		assert hasMap.equals(fetched);
	}

	@Test
	public void testNestedEmbeddedMap() throws Exception {
		this.fact.register(HasNestedMapEmbed.class);
		
		HasNestedMapEmbed hasNested = new HasNestedMapEmbed();
		
		HasMapEmbed hasMap1 = createHasMapEmbed(1);
		HasMapEmbed hasMap2 = createHasMapEmbed(2);

		hasNested.nestedThings.put("one", hasMap1);
		hasNested.nestedThings.put("two", hasMap2);

		HasNestedMapEmbed fetched = this.putClearGet(hasNested);
		
		assert fetched.nestedThings.equals(hasNested.nestedThings);
	}
}
