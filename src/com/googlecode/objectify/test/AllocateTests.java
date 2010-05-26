/*
 * $Id: BeanMixin.java 1075 2009-05-07 06:41:19Z lhoriman $
 * $URL: https://subetha.googlecode.com/svn/branches/resin/rtest/src/org/subethamail/rtest/util/BeanMixin.java $
 */

package com.googlecode.objectify.test;

import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.KeyRange;
import com.googlecode.objectify.test.entity.Child;
import com.googlecode.objectify.test.entity.Trivial;

/**
 * Tests of simple key allocations
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class AllocateTests extends TestBase
{
	/** */
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(AllocateTests.class);
	
	/** */
	@Test
	public void testBasicAllocation() throws Exception
	{
		KeyRange<Trivial> range = this.fact.allocateIds(Trivial.class, 5);
		
		Iterator<Key<Trivial>> it = range.iterator();
		
		long previousId = 0;
		for (int i=0; i<5; i++)
		{
			Key<Trivial> next = it.next();
			assert next.getId() > previousId;
			previousId = next.getId();
		}
		
		// Create an id with a put and verify it is > than the last
		Trivial triv = new Trivial("foo", 3);
		this.fact.begin().put(triv);
		
		assert triv.getId() > previousId;
	}
	
	/** */
	@Test
	public void testParentAllocation() throws Exception
	{
		Key<Trivial> parentKey = new Key<Trivial>(Trivial.class, 123);
		KeyRange<Child> range = this.fact.allocateIds(parentKey, Child.class, 5);
		
		Iterator<Key<Child>> it = range.iterator();
		
		long previousId = 0;
		for (int i=0; i<5; i++)
		{
			Key<Child> next = it.next();
			assert next.getId() > previousId;
			previousId = next.getId();
		}
		
		// Create an id with a put and verify it is > than the last
		Child ch = new Child(parentKey, "foo");
		this.fact.begin().put(ch);
		
		assert ch.getId() > previousId;
	}
}