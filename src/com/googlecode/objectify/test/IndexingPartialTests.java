/*
 * $Id: BeanMixin.java 1075 2009-05-07 06:41:19Z lhoriman $
 * $URL: https://subetha.googlecode.com/svn/branches/resin/rtest/src/org/subethamail/rtest/util/BeanMixin.java $
 */

package com.googlecode.objectify.test;

import javax.persistence.Id;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.annotation.Cached;
import com.googlecode.objectify.annotation.Unindexed;
import com.googlecode.objectify.condition.IfFalse;

/**
 * Tests of partial indexing.  Doesn't stress test the If mechanism; that is
 * checked in the UnsavedTests.
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class IndexingPartialTests extends TestBase
{
	/** */
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(IndexingPartialTests.class);
	
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
	
}