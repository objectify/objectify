/*
 * $Id: BeanMixin.java 1075 2009-05-07 06:41:19Z lhoriman $
 * $URL: https://subetha.googlecode.com/svn/branches/resin/rtest/src/org/subethamail/rtest/util/BeanMixin.java $
 */

package com.googlecode.objectify.test;

import java.util.logging.Logger;

import javax.persistence.Id;

import org.testng.annotations.Test;

import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.annotation.Cached;
import com.googlecode.objectify.annotation.Indexed;
import com.googlecode.objectify.annotation.Unindexed;
import com.googlecode.objectify.condition.IfFalse;
import com.googlecode.objectify.condition.PojoIf;

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
	@Cached
	static class UnindexedWhenFalse
	{
		@Id Long id;
		@Unindexed(IfFalse.class) boolean foo;
	}
	
	/** */
	@Test
	public void testUnindexedWhenFalse() throws Exception
	{
		this.fact.register(UnindexedWhenFalse.class);
		Objectify ofy = this.fact.begin();

		UnindexedWhenFalse thing = new UnindexedWhenFalse();
		
		// Should be able to query for it when true
		thing.foo = true;
		ofy.put(thing);
		assert thing.id == ofy.query(UnindexedWhenFalse.class).filter("foo", true).get().id;

		// Should not be able to query for it when false
		thing.foo = false;
		ofy.put(thing);
		assert !ofy.query(UnindexedWhenFalse.class).filter("foo", true).iterator().hasNext();
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
	@Cached
	@Unindexed
	static class IndexedOnOtherField
	{
		@Id Long id;
		public boolean indexBar;
		public @Indexed(IfComplicated.class) boolean bar;
	}
	
	/** */
	@Test
	public void testUnindexedOnOtherField() throws Exception
	{
		this.fact.register(IndexedOnOtherField.class);
		Objectify ofy = this.fact.begin();

		IndexedOnOtherField thing = new IndexedOnOtherField();
		thing.bar = true;
		
		// Should be able to query for bar when true
		thing.indexBar = true;
		ofy.put(thing);
		assert thing.id == ofy.query(IndexedOnOtherField.class).filter("bar", true).get().id;

		// Should not be able to query for bar when false
		thing.indexBar = false;
		ofy.put(thing);
		assert !ofy.query(IndexedOnOtherField.class).filter("bar", true).iterator().hasNext();
	}
	
}