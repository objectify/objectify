/*
 */

package com.googlecode.objectify.test;

import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.Unindex;
import com.googlecode.objectify.condition.IfFalse;
import com.googlecode.objectify.condition.PojoIf;
import com.googlecode.objectify.test.util.TestBase;
import org.testng.annotations.Test;

import java.util.logging.Logger;

import static com.googlecode.objectify.test.util.TestObjectifyService.fact;
import static com.googlecode.objectify.test.util.TestObjectifyService.ofy;

/**
 * Tests of partial indexing.  Doesn't stress test the If mechanism; that is
 * checked in the NotSavedTests.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class IndexingPartialTests extends TestBase
{
	/** */
	@SuppressWarnings("unused")
	private static Logger log = Logger.getLogger(IndexingPartialTests.class.getName());

	/** */
	public static final String TEST_VALUE = "blah";

	/** */
	@Entity
	@Cache
	@Index
	static class UnindexedWhenFalse
	{
		@Id Long id;
		@Unindex(IfFalse.class) boolean foo;
	}

	/** */
	@Test
	public void testUnindexedWhenFalse() throws Exception
	{
		fact().register(UnindexedWhenFalse.class);

		UnindexedWhenFalse thing = new UnindexedWhenFalse();

		// Should be able to query for it when true
		thing.foo = true;
		ofy().save().entity(thing).now();
		assert thing.id == ofy().load().type(UnindexedWhenFalse.class).filter("foo", true).first().now().id;

		// Should not be able to query for it when false
		thing.foo = false;
		ofy().save().entity(thing).now();
		assert !ofy().load().type(UnindexedWhenFalse.class).filter("foo", true).iterator().hasNext();
	}

	/** */
	static class IfComplicated extends PojoIf<IndexedOnOtherField>
	{
		@Override
		public boolean matchesPojo(IndexedOnOtherField pojo)
		{
			return pojo.indexBar;
		}
	}

	/** */
	@Entity
	@Cache
	@Unindex
	static class IndexedOnOtherField
	{
		@Id Long id;
		public boolean indexBar;
		public @Index(IfComplicated.class) boolean bar;
	}

	/** */
	@Test
	public void testUnindexedOnOtherField() throws Exception
	{
		fact().register(IndexedOnOtherField.class);

		IndexedOnOtherField thing = new IndexedOnOtherField();
		thing.bar = true;

		// Should be able to query for bar when true
		thing.indexBar = true;
		ofy().save().entity(thing).now();
		assert thing.id == ofy().load().type(IndexedOnOtherField.class).filter("bar", true).first().now().id;

		// Should not be able to query for bar when false
		thing.indexBar = false;
		ofy().save().entity(thing).now();
		assert !ofy().load().type(IndexedOnOtherField.class).filter("bar", true).iterator().hasNext();
	}

}