/*
 */

package com.googlecode.objectify.test;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.cmd.Query;
import com.googlecode.objectify.test.entity.User;
import com.googlecode.objectify.test.util.TestBase;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import static com.googlecode.objectify.test.util.TestObjectifyService.fact;
import static com.googlecode.objectify.test.util.TestObjectifyService.ofy;

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
	@Entity
	public static class HasDate
	{
		@Id Long id;
		@Index Date when;
	}

	/** */
	@Test
	public void testDateFiltering() throws Exception
	{
		fact().register(HasDate.class);

		long later = System.currentTimeMillis();
		long earlier = later - 100000;

		HasDate hd = new HasDate();
		hd.when = new Date(later);

		ofy().save().entity(hd).now();

		Query<HasDate> q = ofy().load().type(HasDate.class).filter("when >", new Date(earlier));

		List<HasDate> result = q.list();

		assert result.size() == 1;
		assert result.get(0).when.equals(hd.when);
	}

	/** */
	@Entity
	public static class HasUser
	{
		@Id Long id;
		com.google.appengine.api.users.User who;
	}

	/** */
	@Test
	public void testUserFiltering() throws Exception
	{
		fact().register(HasUser.class);

		HasUser hd = new HasUser();
		hd.who = new com.google.appengine.api.users.User("samiam@gmail.com", "gmail.com");

		ofy().save().entity(hd).now();

		Query<HasUser> q = ofy().load().type(HasUser.class).filter("who", hd.who);

		List<HasUser> result = q.list();

		// This test doesn't work anymore because properties default to @Unindex as of Ofy4.
		// TODO:  re-enable it when we have a @Metamodel annotation that lets us define indexing in a separate class.
		assert result.size() == 0;

//		assert result.size() == 1;
//		assert result.get(0).who.getEmail().equals(hd.who.getEmail());
	}

	/** */
	@Test
	public void testBadlyNamedUserFiltering() throws Exception
	{
		fact().register(User.class);

		User hd = new User();
		hd.who = new com.google.appengine.api.users.User("samiam@gmail.com", "gmail.com");

		ofy().save().entity(hd).now();

		Query<User> q = ofy().load().type(User.class).filter("who", hd.who);

		List<User> result = q.list();

		// This test doesn't work anymore because properties default to @Unindex as of Ofy4.
		// TODO:  re-enable it when we have a @Metamodel annotation that lets us define indexing in a separate class.
		assert result.size() == 0;

//		assert result.size() == 1;
//		assert result.get(0).who.getEmail().equals(hd.who.getEmail());
	}

	/**
	 * @author aswath satrasala
	 */
	@Entity
	public static class HasFromThruDate
	{
		@Id Long id;
		@Index List<Date> dateList = new ArrayList<Date>();
	}

	/**
	 * @author aswath satrasala
	 */
	@Test
	public void testFromThruDateFiltering() throws Exception
	{
		fact().register(HasFromThruDate.class);

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

		ofy().save().entities(h1, h2, h3).now();

		cal1.set(2010, 7, 25);
		Date fromDate = cal1.getTime();

		cal1.set(2010, 7, 26);
		Date thruDate = cal1.getTime();
		
//		List<com.google.appengine.api.datastore.Entity> ents = new ArrayList<>(ds().prepare(new com.google.appengine.api.datastore.Query()).asList(FetchOptions.Builder.withDefaults()));

		Query<HasFromThruDate> q =
			ofy().load().type(HasFromThruDate.class)
				.filter("dateList >=", fromDate).filter("dateList <=", thruDate);

		List<HasFromThruDate> listresult = q.list();
		assert listresult.size() == 2;
	}
}
