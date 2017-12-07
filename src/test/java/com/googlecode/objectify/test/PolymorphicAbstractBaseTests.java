/*
 */

package com.googlecode.objectify.test;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.test.util.TestBase;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static com.googlecode.objectify.ObjectifyService.factory;

/**
 * Test that we can have abstract base classes in a polymorphic hierarchy.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
class PolymorphicAbstractBaseTests extends TestBase {

	/** */
	@Entity
	@Data
	abstract private static class Base {
		@Id Long id;
		String foo;
	}

	/** */
	@com.googlecode.objectify.annotation.Subclass(index=true)
	@Data
	@EqualsAndHashCode(callSuper = true)
	private static class Subclass extends Base {
		boolean bar;
	}

	/** */
	@Test
	void registersForwards() throws Exception {
		factory().register(Base.class);
		factory().register(Subclass.class);
	}

	/** */
	@Test
	void registersBackwards() throws Exception {
		factory().register(Subclass.class);
		factory().register(Base.class);
	}

	/** */
	@Test
	void basicFetch() throws Exception {
		this.registersForwards();

		final Subclass sub = new Subclass();
		sub.foo = "foo";
		sub.bar = true;

		final Subclass fetched = saveClearLoad(sub);
		assertThat(fetched).isEqualTo(sub);
	}
}
