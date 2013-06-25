/*
 */

package com.googlecode.objectify.test;

import static com.googlecode.objectify.test.util.TestObjectifyService.fact;

import org.testng.annotations.Test;

import com.googlecode.objectify.test.entity.Trivial;
import com.googlecode.objectify.test.util.TestBase;

/**
 * Tests of KeyMetadata
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class KeyMetadataTests extends TestBase
{
	/** */
	@Test
	public void testIdType() throws Exception {
		fact().register(Trivial.class);
		assert fact().getMetadata(Trivial.class).getKeyMetadata().getIdFieldType() == Long.class;
	}
}