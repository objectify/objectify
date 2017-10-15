/*
 */

package com.googlecode.objectify.test;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.test.util.TestBase;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;

/**
 * Tests of Key behavior
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
class KeyTests extends TestBase {
	private static class NoEntity {
	}

	/** */
	@Test
	void kindOfClassWithoutEntityAnnotationIsClassSimpleName() throws Exception {
		final Key<NoEntity> key = Key.create(NoEntity.class, 123L);
		assertThat(key.getKind()).isEqualTo(NoEntity.class.getSimpleName());
	}
}