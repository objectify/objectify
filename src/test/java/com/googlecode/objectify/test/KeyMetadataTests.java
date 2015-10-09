/*
 */

package com.googlecode.objectify.test;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Parent;
import com.googlecode.objectify.test.entity.Trivial;
import com.googlecode.objectify.test.util.TestBase;
import org.testng.annotations.Test;

import static com.googlecode.objectify.test.util.TestObjectifyService.fact;
import static com.googlecode.objectify.test.util.TestObjectifyService.ofy;

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

	static class BaseThing {
		@Parent Key<?> parent;
		@Id Long id;
	}

	@Entity
	static class Thing extends BaseThing {
		String foo;
	}

	/**
	 */
	@Test
	public void idAndParentCanBeSpecifiedInBaseClass() throws Exception {
		fact().register(Thing.class);

		Thing th = new Thing();
		th.parent = Key.create(Thing.class, 123L);
		th.id = 456L;

		Thing fetched = ofy().saveClearLoad(th);
		assert fetched.parent.equals(th.parent);
		assert fetched.id.equals(th.id);
	}
}
