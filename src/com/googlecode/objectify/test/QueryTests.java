/*
 * $Id: BeanMixin.java 1075 2009-05-07 06:41:19Z lhoriman $
 * $URL: https://subetha.googlecode.com/svn/branches/resin/rtest/src/org/subethamail/rtest/util/BeanMixin.java $
 */

package com.googlecode.objectify.test;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.googlecode.objectify.ObjPreparedQuery;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.test.entity.Trivial;

/**
 * Tests of various queries
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class QueryTests extends TestBase
{
	/** */
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(QueryTests.class);
	
	/** */
	@Test
	public void testKeysOnly() throws Exception
	{
		Objectify ofy = ObjectifyFactory.begin();
		
		Trivial triv1 = new Trivial("foo1", 1);
		Trivial triv2 = new Trivial("foo2", 2);
		List<Trivial> trivs = new ArrayList<Trivial>();
		trivs.add(triv1);
		trivs.add(triv2);
		
		List<Key> keys = ofy.put(trivs);

		Query q = ObjectifyFactory.createQuery(Trivial.class);
		q.setKeysOnly();
		
		ObjPreparedQuery<Key> pq = ofy.prepare(q);
		
		int count = 0;
		for (Key k: pq.asIterable())
		{
			assert keys.contains(k);
			count++;
		}
		
		assert count == trivs.size();
		
		// Just for the hell of it, test the other methods
		for (Key k: pq.asList(FetchOptions.Builder.withLimit(1000)))
			assert keys.contains(k);
		
		assert pq.count() == trivs.size();
		
		try
		{
			pq.asSingle();
			assert false: "Should not be able to asSingle() when there are multiple results";
		}
		catch (PreparedQuery.TooManyResultsException ex) {}
	}

}