/*
 * $Id: BeanMixin.java 1075 2009-05-07 06:41:19Z lhoriman $
 * $URL: https://subetha.googlecode.com/svn/branches/resin/rtest/src/org/subethamail/rtest/util/BeanMixin.java $
 */

package com.googlecode.objectify.test;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import org.testng.annotations.Test;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.Unindex;
import com.googlecode.objectify.test.util.TestBase;
import com.googlecode.objectify.test.util.TestObjectify;

/**
 * Testing of field visiblity (package/private/etc)
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class FieldVisibilityTests extends TestBase
{
	/** */
	@SuppressWarnings("unused")
	private static Logger log = Logger.getLogger(FieldVisibilityTests.class.getName());
	
	/** */
	@Entity
	@Unindex
	static class ThingWithPrivates
	{
		@Id
		private Long id;
		
		@Index
		private Set<String> stuff = new HashSet<String>();
	}
	
	/** */
	@Test
	public void testGet() throws Exception
	{
		this.fact.register(ThingWithPrivates.class);
		
		ThingWithPrivates thing = new ThingWithPrivates();
		//thing.stuff.add("foo");
		
		ThingWithPrivates fetched = this.putClearGet(thing);
		
		assert fetched.id.equals(thing.id);
	}

	/** */
	@Test
	public void testQuery() throws Exception
	{
		this.fact.register(ThingWithPrivates.class);
		
		TestObjectify ofy = this.fact.begin();
		
		ThingWithPrivates thing = new ThingWithPrivates();
		thing.stuff.add("foo");
		
		ofy.put(thing);
		
		ofy.load().type(ThingWithPrivates.class).filter("stuff", "foo").list();
	}
}