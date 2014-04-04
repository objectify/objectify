/*
 */

package com.googlecode.objectify.test;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.googlecode.objectify.test.entity.Trivial;
import com.googlecode.objectify.test.util.TestBase;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static com.googlecode.objectify.test.util.TestObjectifyService.ds;
import static com.googlecode.objectify.test.util.TestObjectifyService.fact;
import static com.googlecode.objectify.test.util.TestObjectifyService.ofy;

/**
 * Tests of basic query operations
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class QueryBasicTests extends TestBase
{
	/** */
	@SuppressWarnings("unused")
	private static Logger log = Logger.getLogger(QueryBasicTests.class.getName());

	/** */
	@Test
	public void simpleQueryWorks() throws Exception {
		fact().register(Trivial.class);

		Trivial triv = new Trivial(123L, "foo", 12);
		ofy().save().entity(triv).now();

		Query q = new Query("Trivial");
		PreparedQuery pq = ds().prepare(q);
		List<Entity> stuff = pq.asList(FetchOptions.Builder.withDefaults());
		assert stuff.size() == 1;

		int count = 0;
		for (@SuppressWarnings("unused") Trivial fetched: ofy().load().type(Trivial.class)) {
			count++;
		}
		assert count == 1;
	}

	/** */
	@Test
	public void testChunking() throws Exception {
		fact().register(Trivial.class);

		List<Trivial> trivs = new ArrayList<Trivial>(100);
		for (int i = 0; i < 100; i++) {
			Trivial triv = new Trivial(1000L + i, "foo" + i, i);
			trivs.add(triv);
		}

		ofy().save().entities(trivs).now();

		assert trivs.size() == 100;

		int count = 0;
		for (Trivial triv: ofy().load().type(Trivial.class).chunk(2)) {
			assert triv.getSomeNumber() == count;
			count++;
		}
		assert count == 100;
	}
}