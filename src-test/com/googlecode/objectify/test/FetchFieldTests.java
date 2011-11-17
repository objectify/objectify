/*
 */

package com.googlecode.objectify.test;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Id;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Load;
import com.googlecode.objectify.test.entity.Trivial;
import com.googlecode.objectify.test.util.TestBase;
import com.googlecode.objectify.test.util.TestObjectify;

/**
 * Tests the fetching system for simple parent values.
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class FetchFieldTests extends TestBase
{
	Trivial t1;
	Trivial t2;
	Trivial tNone1;
	Trivial tNone2;
	Key<Trivial> k1;
	Key<Trivial> k2;
	Key<Trivial> kNone1;
	Key<Trivial> kNone2;
	
	/** */
	@BeforeMethod
	public void createTwo() {
		TestObjectify ofy = fact.begin();
		
		t1 = new Trivial("foo", 11);
		k1 = ofy.put(t1);
		
		t2 = new Trivial("bar", 22);
		k2 = ofy.put(t2);
		
		tNone1 = new Trivial(123L, "fooNone", 33);
		tNone2 = new Trivial(456L, "barNone", 44);
		
		kNone1 = fact.getKey(tNone1);
		kNone2 = fact.getKey(tNone2);
	}

	/** */
	public static class HasEntities {
		public @Id Long id;
		public @Load Trivial single;
		public @Load List<Trivial> multi = new ArrayList<Trivial>();
	}
	
	/** */
	@Test
	public void testTargetsExist() throws Exception
	{
		fact.register(HasEntities.class);
		
		HasEntities he = new HasEntities();
		he.single = t1;
		he.multi.add(t1);
		he.multi.add(t2);
		HasEntities fetched = this.putAndGet(he);
		
		assert fetched.single.getId().equals(t1.getId());
		assert fetched.single.getSomeString().equals(t1.getSomeString());
		
		assert fetched.multi.get(0) == fetched.single;
		
		assert fetched.multi.get(1).getId().equals(t2.getId());
		assert fetched.multi.get(1).getSomeString().equals(t2.getSomeString());
	}

	/** */
	@Test
	public void testTargetsDontExist() throws Exception
	{
		fact.register(HasEntities.class);
		
		HasEntities he = new HasEntities();
		he.single = tNone1;
		he.multi.add(tNone1);
		he.multi.add(tNone2);
		HasEntities fetched = this.putAndGet(he);
		
		assert fetched.single.getId().equals(tNone1.getId());
		assert fetched.single.getSomeString() == null;
		
		assert fetched.multi.get(0) == fetched.single;
		
		assert fetched.multi.get(1).getId().equals(t2.getId());
		assert fetched.multi.get(1).getSomeString() == null;
	}

	/** */
	public static class ListNode {
		public @Id Long id;
		public @Load ListNode next;
		public String foo;
	}
	
	/** */
	@Test
	public void testTwoLevelsOfFetch() throws Exception
	{
		fact.register(ListNode.class);
		
		TestObjectify ofy = fact.begin();
		
		ListNode node3 = new ListNode();
		node3.foo = "foo3";
		ofy.put(node3);

		ListNode node2 = new ListNode();
		node2.foo = "foo2";
		node2.next = node3;
		ofy.put(node2);

		ListNode node1 = new ListNode();
		node1.foo = "foo1";
		node1.next = node2;
		ofy.put(node1);
		
		ListNode fetched = ofy.get(fact.<ListNode>getKey(node1));
		
		assert fetched.foo.equals(node1.foo);
		assert fetched.next.id.equals(node2.id);
		assert fetched.next.foo.equals(node2.foo);
		assert fetched.next.next.id.equals(node3.id);
		assert fetched.next.next.foo.equals(node3.foo);
		assert fetched.next.next.next == null;
	}

	/** */
	@Test
	public void testMissingTail() throws Exception
	{
		fact.register(ListNode.class);
		
		TestObjectify ofy = fact.begin();
		
		// Node2 should not exist but should have a concrete id for node1
		ListNode node2 = new ListNode();
		node2.id = 999L;

		ListNode node1 = new ListNode();
		node1.foo = "foo1";
		node1.next = node2;
		ofy.put(node1);
		
		ListNode fetched = ofy.get(fact.<ListNode>getKey(node1));
		
		assert fetched.foo.equals(node1.foo);
		assert fetched.next.id.equals(node2.id);
		assert fetched.next.foo == null;
		assert fetched.next.next == null;
	}
	
	/** */
	public static class HasEntitiesWithGroups {
		public @Id Long id;
		public @Load("single") Trivial single;
		public @Load("multi") List<Trivial> multi = new ArrayList<Trivial>();
	}
	
	
	/** */
	@Test
	public void testGrouping() throws Exception
	{
		fact.register(HasEntitiesWithGroups.class);
		
		HasEntitiesWithGroups he = new HasEntitiesWithGroups();
		he.single = t1;
		he.multi.add(t1);
		he.multi.add(t2);
		HasEntitiesWithGroups fetched = this.putAndGet(he);
		
		assert fetched.single.getId().equals(t1.getId());
		assert fetched.single.getSomeString() == null;
		assert fetched.multi.get(0) == fetched.single;
		
		assert fetched.multi.get(1).getId().equals(t2.getId());
		assert fetched.multi.get(1).getSomeString() == null;
		
		TestObjectify ofy = fact.begin();
		
		fetched = ofy.load().group("single").entity(fact.<HasEntitiesWithGroups>getKey(he)).get();
		assert fetched.single.getId().equals(t1.getId());
		assert fetched.single.getSomeString().equals(t1.getSomeString());
		assert fetched.multi.get(0) == fetched.single;	// good question about this
		assert fetched.multi.get(1).getId().equals(t2.getId());
		assert fetched.multi.get(1).getSomeString() == null;

		fetched = ofy.load().group("multi").entity(fact.<HasEntitiesWithGroups>getKey(he)).get();
		assert fetched.multi.get(0).getId().equals(t1.getId());
		assert fetched.multi.get(0).getSomeString().equals(t1.getSomeString());
		assert fetched.multi.get(1).getId().equals(t2.getId());
		assert fetched.multi.get(1).getSomeString().equals(t2.getSomeString());
		assert fetched.single.getId().equals(t1.getId());
		assert fetched.single.getSomeString() == null;	// or should this be the same item as multi[0]?
		
		fetched = ofy.load().group("single").group("multi").entity(fact.<HasEntitiesWithGroups>getKey(he)).get();
		assert fetched.multi.get(0).getId().equals(t1.getId());
		assert fetched.multi.get(0).getSomeString().equals(t1.getSomeString());
		assert fetched.multi.get(1).getId().equals(t2.getId());
		assert fetched.multi.get(1).getSomeString().equals(t2.getSomeString());
		assert fetched.single == fetched.multi.get(0);
	}
}