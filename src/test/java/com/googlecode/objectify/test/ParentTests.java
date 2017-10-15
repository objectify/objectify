/*
 */

package com.googlecode.objectify.test;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Parent;
import com.googlecode.objectify.test.util.TestBase;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static com.googlecode.objectify.ObjectifyService.factory;

/**
 * Tests the fetching system for simple parent values.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
class ParentTests extends TestBase {
	/** */
	@Entity
	@Data
	@NoArgsConstructor
	private static class Father {
		@Id Long id;
		String foo;

		Father(long id) { this.id = id; }
	}

	/** */
	@Entity
	@Data
	@NoArgsConstructor
	private static class KeyChild {
		@Id Long id;
		@Parent Key<Father> father;
		String bar;
	}

	/** */
	@Entity
	@Data
	@NoArgsConstructor
	private static class RefChild {
		@Id Long id;
		@Parent Ref<Father> father;
		String bar;
	}

	/** */
	@Test
	void basicPersistenceOfChild() throws Exception {
		factory().register(Father.class);
		factory().register(KeyChild.class);

		final KeyChild ch = new KeyChild();
		ch.father = Key.create(Father.class, 123);
		ch.bar = "bar";

		final KeyChild fetched = saveClearLoad(ch);

		assertThat(fetched).isEqualTo(ch);
	}

	/** */
	@Test
	void parentIsRef() throws Exception {
		factory().register(Father.class);
		factory().register(RefChild.class);

		final RefChild ch = new RefChild();
		ch.father = Ref.create(Key.create(Father.class, 123));
		ch.bar = "bar";

		final RefChild fetched = saveClearLoad(ch);

		assertThat(fetched).isEqualTo(ch);
	}
}