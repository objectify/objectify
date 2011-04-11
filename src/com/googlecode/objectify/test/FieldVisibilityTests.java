/*
 * $Id: BeanMixin.java 1075 2009-05-07 06:41:19Z lhoriman $
 * $URL: https://subetha.googlecode.com/svn/branches/resin/rtest/src/org/subethamail/rtest/util/BeanMixin.java $
 */

package com.googlecode.objectify.test;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import javax.persistence.Id;

import org.testng.annotations.Test;

import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.annotation.Indexed;
import com.googlecode.objectify.annotation.Unindexed;

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
	@Unindexed
	static class ThingWithPrivates
	{
		@Id
		private Long id;
		
		@Indexed
		private Set<String> stuff = new HashSet<String>();
	}
	
	/** */
	@Test
	public void testGet() throws Exception
	{
		this.fact.register(ThingWithPrivates.class);
		
		ThingWithPrivates thing = new ThingWithPrivates();
		//thing.stuff.add("foo");
		
		ThingWithPrivates fetched = this.putAndGet(thing);
		
		assert fetched.id.equals(thing.id);
	}

	/** */
	@Test
	public void testQuery() throws Exception
	{
		this.fact.register(ThingWithPrivates.class);
		
		Objectify ofy = this.fact.begin();
		
		ThingWithPrivates thing = new ThingWithPrivates();
		thing.stuff.add("foo");
		
		ofy.put(thing);
		
		ofy.query(ThingWithPrivates.class).filter("stuff", "foo").list();
	}
}