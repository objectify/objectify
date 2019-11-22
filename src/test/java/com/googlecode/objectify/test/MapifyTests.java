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

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.google.common.truth.Truth.assertThat;
import static com.googlecode.objectify.ObjectifyService.factory;
import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * Test persisting of Map with @Mapify annotations
 */
class MapifyTests extends TestBase {

	@Data
	@NoArgsConstructor
	private static class Thing {
		String name;
		Long weight;

		Thing(String name, Long weight) {
			this.name = name;
			this.weight = weight;
		}
	}
	
	@Subclass
	@Data
	@EqualsAndHashCode(callSuper = true)
	@NoArgsConstructor
	private static class ThingSubclass extends Thing {
		ThingSubclass(String name, Long weight) {
			super(name, weight);
		}
	}

	private static class ThingMapper implements Mapper<Long, Thing> {
		@Override
		public Long getKey(Thing value) {
			return value.weight;
		}
	}

	@Entity
	@Data
	private static class HasMapify {
		@Id Long id;

		@Mapify(ThingMapper.class)
		Map<Long, Thing> things = new LinkedHashMap<>();
	}


	@Test
	void basicMapify() throws Exception {
		factory().register(HasMapify.class);

		final HasMapify hasMap = new HasMapify();
		final Thing thing0 = new Thing("foo", 123L);
		hasMap.things.put(thing0.weight, thing0);
		final Thing thing1 = new Thing("bar", 456L);
		hasMap.things.put(thing1.weight, thing1);

		checkTestMapify(hasMap);
	}

	@Test
	void mapifyPolymorphic() throws Exception {
		factory().register(HasMapify.class);
		factory().register(ThingSubclass.class);
		
		HasMapify hasMap = new HasMapify();
		Thing thing0 = new ThingSubclass("foo", 123L);
		hasMap.things.put(thing0.weight, thing0);
		Thing thing1 = new ThingSubclass("bar", 456L);
		hasMap.things.put(thing1.weight, thing1);
		
		checkTestMapify(hasMap);
	}

	private void checkTestMapify(final HasMapify hasMap) {
		final HasMapify fetched = saveClearLoad(hasMap);

		assertThat(fetched).isEqualTo(hasMap);
		assertThat(fetched.things).isInstanceOf(LinkedHashMap.class);
	}

	/** */
	@Entity
	@Data
	@NoArgsConstructor
	private static class Top {
		@Id long id;

		@Mapify(BottomMapper.class)
		Map<String, Bottom> bottoms = new HashMap<>();

		Top(long id) { this.id = id; }
	}

	/** */
	@Data
	private static class Bottom {
		@Load Ref<Top> top;
		String name;
	}

	private static class BottomMapper implements Mapper<String, Bottom> {
		@Override
		public String getKey(Bottom value) {
			assertThat(value.top).isNotNull();	// this is the problem place
			return value.name;
		}
	}

	/**
	 */
	@Test
	void bidirectionalMapify() throws Exception {
		factory().register(Top.class);

		final Top top = new Top(123);

		final Bottom bot = new Bottom();
		bot.name = "foo";
		bot.top = Ref.create(top);

		top.bottoms.put(bot.name, bot);

		final Top topFetched = saveClearLoad(top);
		assertThat(topFetched).isEqualTo(top);

		final Bottom bottomFetched = topFetched.bottoms.get(bot.name);
		assertThat(bottomFetched.top.get()).isSameInstanceAs(topFetched);
		assertThat(bottomFetched).isEqualTo(bot);
	}

	/** */
	private static class TrivialMapper implements Mapper<Key<Trivial>, Ref<Trivial>> {
		@Override
		public Key<Trivial> getKey(Ref<Trivial> value) {
			return value.key();
		}
	}

	@Entity
	@Data
	private static class HasMapifyTrivial {
		@Id Long id;

		@Mapify(TrivialMapper.class)
		@Load
		Map<Key<Trivial>, Ref<Trivial>> trivials = new HashMap<>();
	}

	/** Tests using mapify on refs */
	@Test
	void mapifyTrivialRefs() throws Exception {
		factory().register(Trivial.class);
		factory().register(HasMapifyTrivial.class);

		final Trivial triv = new Trivial("foo", 123L);
		final Key<Trivial> trivKey = ofy().save().entity(triv).now();

		final HasMapifyTrivial hasMap = new HasMapifyTrivial();
		hasMap.trivials.put(trivKey, Ref.create(triv));

		final HasMapifyTrivial fetched = saveClearLoad(hasMap);

		assertThat(fetched).isEqualTo(hasMap);
	}
}
