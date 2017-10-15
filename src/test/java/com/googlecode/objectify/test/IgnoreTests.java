package com.googlecode.objectify.test;

import com.google.appengine.api.datastore.Entity;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Ignore;
import com.googlecode.objectify.test.util.TestBase;
import lombok.Data;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static com.googlecode.objectify.ObjectifyService.factory;

/**
 */
class IgnoreTests extends TestBase {
	@com.googlecode.objectify.annotation.Entity
	@Cache
	@Data
	private static class HasTransients {
		@Id Long id;
		String name;
		transient int transientKeyword;
		@Ignore int ignoreAnnotation;
	}

	/** */
	@Test
	void ignoreFieldsAreIgnored() throws Exception {
		factory().register(HasTransients.class);

		final HasTransients o = new HasTransients();
		o.name = "saved";
		o.transientKeyword = 42;
		o.ignoreAnnotation = 43;

		final HasTransients fetched = saveClearLoad(o);
		assertThat(fetched.name).isEqualTo("saved");
		assertThat(fetched.transientKeyword).isEqualTo(42);	// persisted normally
		assertThat(fetched.ignoreAnnotation).isEqualTo(0);	// would fail without session clear

		final Entity e = ds().get(null, Key.create(fetched).getRaw());

		assertThat(e.getProperties()).containsExactly("name", "saved", "transientKeyword", 42L);
	}
}
