/*
 * $Id: BeanMixin.java 1075 2009-05-07 06:41:19Z lhoriman $
 * $URL: https://subetha.googlecode.com/svn/branches/resin/rtest/src/org/subethamail/rtest/util/BeanMixin.java $
 */

package com.googlecode.objectify.test;

import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import javax.persistence.Id;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.appengine.api.users.User;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.Query;

/**
 * Tests of queries of odd field types.
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class QueryExoticTypesTests extends TestBase
{
	/** */
	@SuppressWarnings("unused")
	private static Logger log = Logger.getLogger(QueryExoticTypesTests.class.getName());

	/** */
	@BeforeMethod
	public void setUp()
	{
		super.setUp();
	}	
	
	/** */
	public static class HasDate
	{
		@Id Long id;
		Date when;
	}
	
	/** */
	@Test
	public void testDateFiltering() throws Exception
	{
		this.fact.register(HasDate.class);
		Objectify ofy = this.fact.begin();
		
		long later = System.currentTimeMillis();
		long earlier = later - 100000;
			
		HasDate hd = new HasDate();
		hd.when = new Date(later);
		
		ofy.put(hd);
			
		Query<HasDate> q = ofy.query(HasDate.class).filter("when >", new Date(earlier));
		
		List<HasDate> result = q.list();
		
		assert result.size() == 1;
		assert result.get(0).when.equals(hd.when);
	}
	
	/** */
	public static class HasUser
	{
		@Id Long id;
		User who;
	}
	
	/** */
	@Test
	public void testUserFiltering() throws Exception
	{
		this.fact.register(HasUser.class);
		Objectify ofy = this.fact.begin();
		
		HasUser hd = new HasUser();
		hd.who = new User("samiam@gmail.com", "gmail.com");
		
		ofy.put(hd);
			
		Query<HasUser> q = ofy.query(HasUser.class).filter("who", hd.who);
		
		List<HasUser> result = q.list();
		
		assert result.size() == 1;
		assert result.get(0).who.getEmail().equals(hd.who.getEmail());
	}
}
