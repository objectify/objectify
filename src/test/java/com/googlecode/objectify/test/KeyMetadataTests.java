/*
 */

package com.googlecode.objectify.test;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Parent;
import com.googlecode.objectify.test.entity.Trivial;
import com.googlecode.objectify.test.util.TestBase;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static com.googlecode.objectify.ObjectifyService.factory;

/**
 * Tests of KeyMetadata
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
class KeyMetadataTests extends TestBase {
	/** */
	@Test
	void testIdType() throws Exception {
		factory().register(Trivial.class);
		assertThat(factory().getMetadata(Trivial.class).getKeyMetadata().getIdFieldType()).isEqualTo(Long.class);
	}

	@Data
	private static class BaseThing {
		@Parent Key<?> parent;
		@Id Long id;
	}

	@Entity
	@Data
	@EqualsAndHashCode(callSuper = true)
	private static class Thing extends BaseThing {
		String foo;
	}

	/**
	 */
	@Test
	void idAndParentCanBeSpecifiedInBaseClass() throws Exception {
		factory().register(Thing.class);

		final Thing th = new Thing();
		th.parent = Key.create(Thing.class, 123L);
		th.id = 456L;

		final Thing fetched = saveClearLoad(th);

		assertThat(fetched).isEqualTo(th);
	}
}