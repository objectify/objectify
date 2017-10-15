package com.googlecode.objectify.test;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.test.entity.Trivial;
import com.googlecode.objectify.test.util.TestBase;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static com.googlecode.objectify.ObjectifyService.factory;

/**
 * Tests of embedding actual @Entity objects inside each other
 */
class EmbeddedEntityTests extends TestBase {
	@Entity
	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	private static class Outer {
		private @Id Long id;
		private Trivial trivial;
	}

	@Test
	void embeddedEntityPreservesKey() throws Exception {
		factory().register(Outer.class);
		factory().register(Trivial.class);

		final Outer outer = new Outer(123L, new Trivial(123L, "foo", 9));

		final Outer fetched = saveClearLoad(outer);

		assertThat(fetched).isEqualTo(outer);
	}

	@Test
	void embeddedEntityAllowsNullKey() throws Exception {
		factory().register(Outer.class);
		factory().register(Trivial.class);

		final Outer outer = new Outer(123L, new Trivial(null, "foo", 9));

		final Outer fetched = saveClearLoad(outer);

		assertThat(fetched).isEqualTo(outer);
	}
}
