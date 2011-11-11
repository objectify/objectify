/*
 */

package com.googlecode.objectify.test;

import javax.persistence.Id;

import org.testng.annotations.Test;

import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Fetch;
import com.googlecode.objectify.annotation.Parent;

/**
 * Tests the fetching system for parent values using Ref<?> holders.
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class FetchParentRefTests extends TestBase
{
	/** */
	public static class Father {
		public @Id Long id;
		public String foo;
	}
	
	/** */
	public static class Child {
		public @Id Long id;
		public @Fetch @Parent Ref<Father> father;
		public String bar;
	}
	
	/** */
	@Test
	public void testParentExists() throws Exception
	{
		fact.register(Father.class);
		fact.register(Child.class);
		
		Objectify ofy = fact.begin();
		
		Father f = new Father();
		f.foo = "foo";
		ofy.put(f);
		
		Child ch = new Child();
		ch.father = Ref.create(fact.<Father>getKey(f));
		ch.bar = "bar";
		ofy.put(ch);
		
		Child fetched = ofy.get(fact.<Child>getKey(ch));
		
		assert fetched.bar.equals(ch.bar);
		assert fetched.father.value().id.equals(f.id);
		assert fetched.father.value().foo.equals(f.foo);
	}

	/** */
	public static class TreeNode {
		public @Id Long id;
		public @Fetch @Parent Ref<TreeNode> parent;
		public String foo;
	}
	
	/** */
	@Test
	public void testTwoLevelsOfFetch() throws Exception
	{
		fact.register(TreeNode.class);
		
		Objectify ofy = fact.begin();
		
		TreeNode node1 = new TreeNode();
		node1.foo = "foo1";
		ofy.put(node1);
		
		TreeNode node2 = new TreeNode();
		node2.parent = Ref.create(fact.<TreeNode>getKey(node1));
		node2.foo = "foo2";
		ofy.put(node2);
		
		TreeNode node3 = new TreeNode();
		node3.parent = Ref.create(fact.<TreeNode>getKey(node2));
		node3.foo = "foo3";
		ofy.put(node3);

		TreeNode fetched3 = ofy.get(fact.<TreeNode>getKey(node3));
		
		assert fetched3.foo.equals(node3.foo);
		assert fetched3.parent.value().id.equals(node2.id);
		assert fetched3.parent.value().foo.equals(node2.foo);
		assert fetched3.parent.value().parent.value().id.equals(node1.id);
		assert fetched3.parent.value().parent.value().foo.equals(node1.foo);
		assert fetched3.parent.value().parent.value().parent == null;
	}

	/** */
	@Test
	public void testMissingIntermediate() throws Exception
	{
		fact.register(TreeNode.class);
		
		Objectify ofy = fact.begin();
		
		TreeNode node1 = new TreeNode();
		node1.foo = "foo1";
		ofy.put(node1);

		// Node2 should not exist but should have a concrete id for node3
		TreeNode node2 = new TreeNode();
		node2.id = 999L;
		node2.parent = Ref.create(fact.<TreeNode>getKey(node1));
		
		TreeNode node3 = new TreeNode();
		node3.parent = Ref.create(fact.<TreeNode>getKey(node2));
		node3.foo = "foo3";
		ofy.put(node3);

		TreeNode fetched3 = ofy.get(fact.<TreeNode>getKey(node3));
		
		assert fetched3.parent.value().id.equals(node2.id);
		assert fetched3.parent.value().foo == null;
		assert fetched3.parent.value().parent.value().id.equals(node1.id);
		assert fetched3.parent.value().parent.value().foo.equals(node1.foo);
		assert fetched3.parent.value().parent.value().parent == null;
	}

	/** */
	public static class ChildWithGroup {
		public @Id Long id;
		public @Fetch("group") @Parent Ref<Father> father;
		public String bar;
	}
	
	/** */
	@Test
	public void testParentWithGroup() throws Exception
	{
		fact.register(Father.class);
		fact.register(ChildWithGroup.class);
		
		Objectify ofy = fact.begin();
		
		Father f = new Father();
		f.foo = "foo";
		ofy.put(f);
		
		ChildWithGroup ch = new ChildWithGroup();
		ch.father = Ref.create(fact.<Father>getKey(f));
		ch.bar = "bar";
		ofy.put(ch);
		
		// This should get an empty ref
		Child fetched = ofy.get(fact.<Child>getKey(ch));
		assert fetched.father.key().getId() == f.id;
		assert fetched.father.value() == null;

		// This should get a filled in ref
		Child fetched2 = ofy.fetch("group").get(fact.<Child>getKey(ch));
		assert fetched2.father.value().id.equals(f.id);
		assert fetched2.father.value().foo.equals(f.foo);
	}
}