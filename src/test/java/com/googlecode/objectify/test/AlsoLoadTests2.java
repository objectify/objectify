/*
 */

package com.googlecode.objectify.test;

import com.google.appengine.api.datastore.Entity;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.TranslateException;
import com.googlecode.objectify.annotation.AlsoLoad;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.IgnoreLoad;
import com.googlecode.objectify.test.util.TestBase;
import lombok.Data;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static com.google.common.truth.Truth.assertThat;
import static com.googlecode.objectify.ObjectifyService.factory;
import static com.googlecode.objectify.ObjectifyService.ofy;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * More tests of using the @AlsoLoad annotation
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
class AlsoLoadTests2 extends TestBase {
	/** */
	private static final String TEST_VALUE = "blah";

	/** */
	@com.googlecode.objectify.annotation.Entity
	@Cache
	@Data
	private static class MethodOverridesField {
		@Id
		private Long id;

		@IgnoreLoad
		private String foo;

		private String bar;
		void set(@AlsoLoad("foo") String overrides) {
			this.bar = overrides;
		}
	}

	/**
	 */
	@BeforeEach
	void setUpExtra() {
		factory().register(MethodOverridesField.class);
	}

	/** */
	@Test
	void alsoLoadMethodsOverrideFields() throws Exception {
		final Entity ent = new Entity(Key.getKind(MethodOverridesField.class));
		ent.setProperty("foo", TEST_VALUE);
		ds().put(ent);

		final Key<MethodOverridesField> key = Key.create(ent.getKey());
		final MethodOverridesField fetched = ofy().load().key(key).now();

		assertThat(fetched.getFoo()).isNull();
		assertThat(fetched.getBar()).isEqualTo(TEST_VALUE);
	}

	@com.googlecode.objectify.annotation.Entity
	private static class HasMap {
		@Id
		private Long id;

		@AlsoLoad("alsoPrimitives")
		private Map<String, Long> primitives = new HashMap<>();
	}

	@Test
	void alsoLoadConflictDetectedForMapFieldsToo() throws Exception {
		factory().register(HasMap.class);

		final Entity ent = new Entity(Key.getKind(HasMap.class));
		ent.setProperty("alsoPrimitives", makeEmbeddedEntityWithProperty("one", 1L));
		ent.setProperty("primitives", makeEmbeddedEntityWithProperty("two", 2L));
		ds().put(ent);

		final Key<HasMap> key = Key.create(ent.getKey());

		assertThrows(TranslateException.class, () -> {
			ofy().load().key(key).now();
		});
	}
}