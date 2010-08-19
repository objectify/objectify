/*
 * $Id: BeanMixin.java 1075 2009-05-07 06:41:19Z lhoriman $
 * $URL: https://subetha.googlecode.com/svn/branches/resin/rtest/src/org/subethamail/rtest/util/BeanMixin.java $
 */

package com.googlecode.objectify.test;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import javax.persistence.Id;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.Query;
import com.googlecode.objectify.test.entity.User;

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
		com.google.appengine.api.users.User who;
	}
	
	/** */
	@Test
	public void testUserFiltering() throws Exception
	{
		this.fact.register(HasUser.class);
		Objectify ofy = this.fact.begin();
		
		HasUser hd = new HasUser();
		hd.who = new com.google.appengine.api.users.User("samiam@gmail.com", "gmail.com");
		
		ofy.put(hd);
			
		Query<HasUser> q = ofy.query(HasUser.class).filter("who", hd.who);
		
		List<HasUser> result = q.list();
		
		assert result.size() == 1;
		assert result.get(0).who.getEmail().equals(hd.who.getEmail());
	}

	/** */
	@Test
	public void testBadlyNamedUserFiltering() throws Exception
	{
		this.fact.register(User.class);
		Objectify ofy = this.fact.begin();
		
		User hd = new User();
		hd.who = new com.google.appengine.api.users.User("samiam@gmail.com", "gmail.com");
		
		ofy.put(hd);
			
		Query<User> q = ofy.query(User.class).filter("who", hd.who);
		
		List<User> result = q.list();
		
		assert result.size() == 1;
		assert result.get(0).who.getEmail().equals(hd.who.getEmail());
	}

	/**
	 * @author aswath satrasala
	 */
	public static class HasFromThruDate
	{
		@Id
		Long id;
		List<Date> dateList = new ArrayList<Date>();
	}

	/**
	 * @author aswath satrasala
	 */
	@Test
	public void testFromThruDateFiltering() throws Exception
	{
		this.fact.register(HasFromThruDate.class);
		Objectify ofy = this.fact.begin();

		HasFromThruDate h1 = new HasFromThruDate();
		Calendar cal1 = Calendar.getInstance();
		cal1.set(2010, 7, 25);
		h1.dateList.add(cal1.getTime());
		cal1.set(2010, 7, 25);
		h1.dateList.add(cal1.getTime());

		HasFromThruDate h2 = new HasFromThruDate();
		cal1.set(2010, 7, 26);
		h2.dateList.add(cal1.getTime());
		cal1.set(2010, 7, 26);
		h2.dateList.add(cal1.getTime());

		HasFromThruDate h3 = new HasFromThruDate();
		cal1.set(2010, 7, 27);
		h3.dateList.add(cal1.getTime());
		cal1.set(2010, 7, 27);
		h3.dateList.add(cal1.getTime());

		ofy.put(h1, h2, h3);

		cal1.set(2010, 7, 25);
		Date fromDate = cal1.getTime();

		cal1.set(2010, 7, 26);
		Date thruDate = cal1.getTime();

		Query<HasFromThruDate> q = ofy.query(HasFromThruDate.class);
		q.filter("dateList >=", fromDate).filter("dateList <=", thruDate);

		List<HasFromThruDate> listresult = q.list();
		assert listresult.size() == 2;
	}
}
