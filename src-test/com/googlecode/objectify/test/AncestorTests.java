/*
 */

package com.googlecode.objectify.test;

import java.util.logging.Logger;

import org.testng.annotations.Test;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.test.entity.Child;
import com.googlecode.objectify.test.entity.Trivial;
import com.googlecode.objectify.test.util.TestBase;
import com.googlecode.objectify.test.util.TestObjectify;

/**
 * Tests of ancestor relationships.
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class AncestorTests extends TestBase
{
	/** */
	@SuppressWarnings("unused")
	private static Logger log = Logger.getLogger(AncestorTests.class.getName());
	
	/** */
	@Test
	public void testSimpleParentChild() throws Exception
	{
		fact.register(Trivial.class);
		TestObjectify ofy = this.fact.begin();
		
		Trivial triv = new Trivial("foo", 5);
		Key<Trivial> parentKey = ofy.put().entity(triv).now();

		Child child = new Child(parentKey, "cry");
		Key<Child> childKey = ofy.put().entity(child).now();
		
		assert childKey.getParent().equals(parentKey);
		
		Child fetched = ofy.load().entity(childKey).get();
		
		assert fetched.getParent().equals(child.getParent());
		assert fetched.getChildString().equals(child.getChildString());
		
		// Let's make sure we can get it back from an ancestor query
		Child queried = ofy.load().type(Child.class).ancestor(parentKey).first().get();
		
		assert queried.getParent().equals(child.getParent());
		assert queried.getChildString().equals(child.getChildString());
	}
}