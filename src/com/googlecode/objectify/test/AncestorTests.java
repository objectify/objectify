/*
 * $Id: BeanMixin.java 1075 2009-05-07 06:41:19Z lhoriman $
 * $URL: https://subetha.googlecode.com/svn/branches/resin/rtest/src/org/subethamail/rtest/util/BeanMixin.java $
 */

package com.googlecode.objectify.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import com.google.appengine.api.datastore.Key;
import com.googlecode.objectify.ObPreparedQuery;
import com.googlecode.objectify.ObQuery;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.test.entity.Child;
import com.googlecode.objectify.test.entity.Trivial;

/**
 * Tests of ancestor relationships.
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class AncestorTests extends TestBase
{
	/** */
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(AncestorTests.class);
	
	/** */
	@Test
	public void testSimpleParentChild() throws Exception
	{
		Objectify ofy = ObjectifyFactory.begin();
		
		Trivial triv = new Trivial("foo", 5);
		Key parentKey = ofy.put(triv);

		Child child = new Child(parentKey, "cry");
		Key childKey = ofy.put(child);
		
		assert childKey.getParent().equals(parentKey);
		
		Child fetched = ofy.get(childKey);
		
		assert fetched.getParent().equals(child.getParent());
		assert fetched.getChildString().equals(child.getChildString());
		
		// Let's make sure we can get it back from an ancestor query
		ObQuery q = ObjectifyFactory.createQuery(Child.class).ancestor(parentKey);
		ObPreparedQuery<Child> pq = ofy.prepare(q);
		Child queried = pq.asSingle();
		
		assert queried.getParent().equals(child.getParent());
		assert queried.getChildString().equals(child.getChildString());
	}
}