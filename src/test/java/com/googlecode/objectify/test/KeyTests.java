/*
 */

package com.googlecode.objectify.test;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.test.util.TestBase;
import org.testng.annotations.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Tests of Key behavior
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class KeyTests extends TestBase
{
	static class NoEntity {
	}

	/** */
	@Test
	public void kindOfClassWithoutEntityAnnotationIsClassSimpleName() throws Exception {
		final Key<NoEntity> key = Key.create(NoEntity.class, 123L);
		assertThat(key.getKind(), equalTo(NoEntity.class.getSimpleName()));
	}
}
