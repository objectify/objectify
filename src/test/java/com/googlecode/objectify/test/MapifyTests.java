package com.googlecode.objectify.test;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Load;
import com.googlecode.objectify.annotation.Mapify;
import com.googlecode.objectify.annotation.Subclass;
import com.googlecode.objectify.mapper.Mapper;
import com.googlecode.objectify.test.entity.Trivial;
import com.googlecode.objectify.test.util.TestBase;

import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.googlecode.objectify.test.util.TestObjectifyService.fact;
import static com.googlecode.objectify.test.util.TestObjectifyService.ofy;

/**
 * Test persisting of Map with @Mapify annotations
 */
public class MapifyTests extends TestBase
{
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

	@Subclass
	public static class ThingSubclass extends Thing {

		public ThingSubclass() { }

		public ThingSubclass(String name, Long weight) {
			super(name, weight);
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
		Map<Long, Thing> things = new LinkedHashMap<>();
	}


	@Test
	public void testMapify() throws Exception {
		fact().register(HasMapify.class);

		HasMapify hasMap = new HasMapify();
		Thing thing0 = new Thing("foo", 123L);
		hasMap.things.put(thing0.weight, thing0);
		Thing thing1 = new Thing("bar", 456L);
		hasMap.things.put(thing1.weight, thing1);

		checkTestMapify(hasMap);
	}

	@Test
	public void testMapifyPolymorphic() throws Exception {
		fact().register(HasMapify.class);
		fact().register(ThingSubclass.class);

		HasMapify hasMap = new HasMapify();
		Thing thing0 = new ThingSubclass("foo", 123L);
		hasMap.things.put(thing0.weight, thing0);
		Thing thing1 = new ThingSubclass("bar", 456L);
		hasMap.things.put(thing1.weight, thing1);

		checkTestMapify(hasMap);
	}

	private void checkTestMapify(HasMapify hasMap) {
		HasMapify fetched = ofy().saveClearLoad(hasMap);

		assert hasMap.things.equals(fetched.things);

		assert fetched.things instanceof LinkedHashMap;
	}


	/** */
	@Entity
	public static class Top {
		public @Id long id;

		@Mapify(BottomMapper.class)
		public Map<String, Bottom> bottoms = new HashMap<>();

		public Top() {}
		public Top(long id) { this.id = id; }
	}

	/** */
	public static class Bottom {
		public @Load Ref<Top> top;
		public String name;
		public Bottom() {}
	}

	public static class BottomMapper implements Mapper<String, Bottom> {
		@Override
		public String getKey(Bottom value) {
			assert value.top != null;	// this is the problem place
			return value.name;
		}
	}

	/**
	 * This is a perverse case that gives nasty trouble.  It is known to fail.  Fortunately it's an unusual case.
	 */
	@Test
	public void testBidirectionalMapify() throws Exception
	{
		fact().register(Top.class);

		Top top = new Top(123);

		Bottom bot = new Bottom();
		bot.name = "foo";
		bot.top = Ref.create(top);

		top.bottoms.put(bot.name, bot);

		ofy().save().entity(top).now();
		ofy().clear();

		Top topFetched = ofy().load().entity(top).now();
		assert topFetched.bottoms.size() == 1;

		Bottom bottomFetched = topFetched.bottoms.get(bot.name);
		assert bottomFetched.top.get().id == top.id;
		assert bottomFetched.name.equals(bot.name);
	}

	/** */
	public static class TrivialMapper implements Mapper<Key<Trivial>, Ref<Trivial>> {
		@Override
		public Key<Trivial> getKey(Ref<Trivial> value) {
			return value.key();
		}
	}

	@com.googlecode.objectify.annotation.Entity
	public static class HasMapifyTrivial {
		@Id Long id;

		@Mapify(TrivialMapper.class)
		@Load
		Map<Key<Trivial>, Ref<Trivial>> trivials = new HashMap<>();
	}

	/** Tests using mapify on entities */
	@Test
	public void testMapifyTrivials() throws Exception {
		fact().register(Trivial.class);
		fact().register(HasMapifyTrivial.class);

		Trivial triv = new Trivial("foo", 123L);
		Key<Trivial> trivKey = ofy().save().entity(triv).now();

		HasMapifyTrivial hasMap = new HasMapifyTrivial();
		hasMap.trivials.put(trivKey, Ref.create(triv));

		HasMapifyTrivial fetched = ofy().saveClearLoad(hasMap);

		assert hasMap.trivials.get(trivKey).get().getSomeString().equals(fetched.trivials.get(trivKey).get().getSomeString());
	}
}
