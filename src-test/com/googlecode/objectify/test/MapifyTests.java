package com.googlecode.objectify.test;

import java.util.HashMap;
import java.util.Map;

import org.testng.annotations.Test;

import com.googlecode.objectify.annotation.Embed;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Mapify;
import com.googlecode.objectify.mapper.Mapper;
import com.googlecode.objectify.test.util.TestBase;

/**
 * Test persisting of Map with @Mapify annotations
 */
public class MapifyTests extends TestBase
{
	@Embed
	public static class Thing {
		String name;
		Long weight;

		public Thing() {}
		public Thing(String name, Long weight) {
			this.name = name;
			this.weight = weight;
		}
		
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
	
	public static class ThingMapper implements Mapper<Long, Thing> {
		@Override
		public Long getKey(Thing value) {
			return value.weight;
		}
	}
	
	@com.googlecode.objectify.annotation.Entity
	public static class HasMapify {
		@Id Long id;
		
		@Mapify(ThingMapper.class)
		Map<Long, Thing> things = new HashMap<Long, Thing>();
	}


	@Test
	public void testMapify() throws Exception {
		this.fact.register(HasMapify.class);

		HasMapify hasMap = new HasMapify();
		Thing thing0 = new Thing("foo", 123L);
		hasMap.things.put(thing0.weight, thing0);
		Thing thing1 = new Thing("bar", 456L);
		hasMap.things.put(thing1.weight, thing1);
		
		HasMapify fetched = this.putClearGet(hasMap);
		
		assert hasMap.things.equals(fetched.things);
	}
}
