package com.googlecode.objectify.test;

import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.FullEntity;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Unindex;
import com.googlecode.objectify.test.util.TestBase;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static com.googlecode.objectify.ObjectifyService.factory;
import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * Test behavior of null fields, and default values
 */
class NullDefaultFieldTests extends TestBase {

	@Data
	@NoArgsConstructor
	private static class Struct {
		String s = "default1";

		Struct(String s)
		{
			this.s = s;
		}
	}

	@com.googlecode.objectify.annotation.Entity
	@Cache
	@Data
	@NoArgsConstructor
	private static class EntityWithDefault {
		@Id
		Long id;
		/** existing property */
		String a;
		/** new property */
		String b = "foo";
		/** new property */
		@Unindex
		String c = "bar";
		/** new embedded */
		Struct s = new Struct("default2");

		EntityWithDefault(String a)
		{
			this.a = a;
		}

		EntityWithDefault(String a, String b, String c) {
			this.a = a;
			this.b = b;
			this.c = c;
		}
	}

	/**
	 * Test that an entity in the datastore with absent fields loads correctly,
	 * when the fields in the entity class have default values
	 */
	@Test
	void newVersionOfEntity() throws Exception {
		factory().register(EntityWithDefault.class);

		// e1 has absent properties
		final FullEntity<?> e1 = makeEntity("EntityWithDefault")
				.set("a", "1")
				.build();

		final Entity complete = datastore().put(e1);
		final com.google.cloud.datastore.Key k1 = complete.getKey();

		final EntityWithDefault o = ofy().load().type(EntityWithDefault.class).id(k1.getId()).now();

		assertThat(o.a).isEqualTo("1");
		assertThat(o.b).isEqualTo("foo");
		assertThat(o.c).isEqualTo("bar");
		assertThat(o.s).isEqualTo(new Struct("default2"));
	}

	/**
	 */
	@Test
	void explicitNullsAreSavedEvenIfFieldsHaveDefaultValues() throws Exception {
		factory().register(EntityWithDefault.class);

		survivesAsIs(new EntityWithDefault("A"));
		survivesAsIs(new EntityWithDefault("A", "B", "C"));
		survivesAsIs(new EntityWithDefault("A", null, null));
	}

	private void survivesAsIs(final EntityWithDefault entity) {
		final EntityWithDefault fetched = saveClearLoad(entity);
		assertThat(fetched).isEqualTo(entity);
	}
}
