/*
 */

package com.googlecode.objectify.test;

import java.util.logging.Logger;

import javax.persistence.Id;

import org.testng.annotations.Test;

import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.Unindex;
import com.googlecode.objectify.condition.IfFalse;
import com.googlecode.objectify.condition.PojoIf;
import com.googlecode.objectify.test.util.TestBase;
import com.googlecode.objectify.test.util.TestObjectify;

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
	@Cache
	static class UnindexedWhenFalse
	{
		@Id Long id;
		@Unindex(IfFalse.class) boolean foo;
	}
	
	/** */
	@Test
	public void testUnindexedWhenFalse() throws Exception
	{
		this.fact.register(UnindexedWhenFalse.class);
		TestObjectify ofy = this.fact.begin();

		UnindexedWhenFalse thing = new UnindexedWhenFalse();
		
		// Should be able to query for it when true
		thing.foo = true;
		ofy.put(thing);
		assert thing.id == ofy.load().type(UnindexedWhenFalse.class).filter("foo", true).first().get().id;

		// Should not be able to query for it when false
		thing.foo = false;
		ofy.put(thing);
		assert !ofy.load().type(UnindexedWhenFalse.class).filter("foo", true).iterator().hasNext();
	}

	/** */
	static class IfComplicated extends PojoIf<IndexedOnOtherField>
	{
		@Override
		public boolean matches(IndexedOnOtherField pojo)
		{
			return pojo.indexBar;
		}
	}
	
	/** */
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
		this.fact.register(IndexedOnOtherField.class);
		TestObjectify ofy = this.fact.begin();

		IndexedOnOtherField thing = new IndexedOnOtherField();
		thing.bar = true;
		
		// Should be able to query for bar when true
		thing.indexBar = true;
		ofy.put(thing);
		assert thing.id == ofy.load().type(IndexedOnOtherField.class).filter("bar", true).first().get().id;

		// Should not be able to query for bar when false
		thing.indexBar = false;
		ofy.put(thing);
		assert !ofy.load().type(IndexedOnOtherField.class).filter("bar", true).iterator().hasNext();
	}
	
}