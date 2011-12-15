/*
 */

package com.googlecode.objectify.test;

import java.util.ArrayList;
import java.util.List;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Load;
import com.googlecode.objectify.test.LoadFieldTests.HasEntitiesWithGroups.Multi;
import com.googlecode.objectify.test.LoadFieldTests.HasEntitiesWithGroups.Single;
import com.googlecode.objectify.test.entity.Trivial;
import com.googlecode.objectify.test.util.TestBase;
import com.googlecode.objectify.test.util.TestObjectify;

/**
 * Tests the fetching system for simple parent values.
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class LoadFieldTests extends TestBase
{
	Trivial t0;
	Trivial t1;
	Trivial tNone0;
	Trivial tNone1;
	Key<Trivial> k0;
	Key<Trivial> k1;
	Key<Trivial> kNone0;
	Key<Trivial> kNone1;
	
	/** */
	@BeforeMethod
	public void createTwo() {
		fact.register(Trivial.class);
		TestObjectify ofy = fact.begin();
		
		t0 = new Trivial("foo", 11);
		k0 = ofy.put(t0);
		
		t1 = new Trivial("bar", 22);
		k1 = ofy.put(t1);
		
		tNone0 = new Trivial(123L, "fooNone", 33);
		tNone1 = new Trivial(456L, "barNone", 44);
		
		kNone0 = fact.getKey(tNone0);
		kNone1 = fact.getKey(tNone1);
	}

	/** */
	@Entity
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
		he.single = t0;
		he.multi.add(t0);
		he.multi.add(t1);
		HasEntities fetched = this.putClearGet(he);
		
		assert fetched.single.getId().equals(t0.getId());
		assert fetched.single.getSomeString().equals(t0.getSomeString());
		
		assert fetched.multi.get(0) == fetched.single;
		
		assert fetched.multi.get(1).getId().equals(t1.getId());
		assert fetched.multi.get(1).getSomeString().equals(t1.getSomeString());
	}

	/** */
	@Test
	public void testTargetsDontExist() throws Exception
	{
		fact.register(HasEntities.class);
		
		HasEntities he = new HasEntities();
		he.single = tNone0;
		he.multi.add(tNone0);
		he.multi.add(tNone1);
		HasEntities fetched = this.putClearGet(he);
		
		assert fetched.single.getId().equals(tNone0.getId());
		assert fetched.single.getSomeString() == null;
		
		assert fetched.multi.get(0).getId().equals(tNone0.getId());
		assert fetched.multi.get(0).getSomeString() == null;
		
		assert fetched.multi.get(1).getId().equals(tNone1.getId());
		assert fetched.multi.get(1).getSomeString() == null;
	}

	/** */
	@Entity
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
	@Entity
	public static class HasEntitiesWithGroups {
		public static class Single {}
		public static class Multi {}
		
		public @Id Long id;
		public @Load(Single.class) Trivial single;
		public @Load(Multi.class) List<Trivial> multi = new ArrayList<Trivial>();
	}
	
	/** */
	@Test
	public void testGrouping() throws Exception
	{
		fact.register(HasEntitiesWithGroups.class);
		
		HasEntitiesWithGroups he = new HasEntitiesWithGroups();
		he.single = t0;
		he.multi.add(t0);
		he.multi.add(t1);
		HasEntitiesWithGroups fetched = this.putClearGet(he);
		
		assert fetched.single.getId().equals(t0.getId());
		assert fetched.single.getSomeString() == null;
		assert fetched.multi.get(0).getId().equals(t0.getId());
		assert fetched.multi.get(1).getId().equals(t1.getId());
		assert fetched.multi.get(1).getSomeString() == null;
		
		TestObjectify ofy = fact.begin();
		
		ofy.clear();
		fetched = ofy.load().group(Single.class).key(fact.<HasEntitiesWithGroups>getKey(he)).get();
		assert fetched.single.getId().equals(t0.getId());
		assert fetched.single.getSomeString().equals(t0.getSomeString());
		assert fetched.multi.get(0).getId().equals(t0.getId());	// or should this be same as single?
		assert fetched.multi.get(0).getSomeString() == null;
		assert fetched.multi.get(1).getId().equals(t1.getId());
		assert fetched.multi.get(1).getSomeString() == null;

		ofy.clear();
		fetched = ofy.load().group(Multi.class).key(fact.<HasEntitiesWithGroups>getKey(he)).get();
		assert fetched.multi.get(0).getId().equals(t0.getId());
		assert fetched.multi.get(0).getSomeString().equals(t0.getSomeString());
		assert fetched.multi.get(1).getId().equals(t1.getId());
		assert fetched.multi.get(1).getSomeString().equals(t1.getSomeString());
		assert fetched.single.getId().equals(t0.getId());
		assert fetched.single.getSomeString() == null;	// or should this be the same item as multi[0]?
		
		ofy.clear();
		fetched = ofy.load().group(Single.class).group(Multi.class).key(fact.<HasEntitiesWithGroups>getKey(he)).get();
		assert fetched.multi.get(0).getId().equals(t0.getId());
		assert fetched.multi.get(0).getSomeString().equals(t0.getSomeString());
		assert fetched.multi.get(1).getId().equals(t1.getId());
		assert fetched.multi.get(1).getSomeString().equals(t1.getSomeString());
		assert fetched.single == fetched.multi.get(0);
	}

}