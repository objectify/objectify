/*
 */

package com.googlecode.objectify.test;

import org.testng.annotations.Test;

import com.googlecode.objectify.Ref;
import com.googlecode.objectify.test.LoadParentRefTests.Child;
import com.googlecode.objectify.test.LoadParentRefTests.ChildWithGroup;
import com.googlecode.objectify.test.LoadParentRefTests.Father;
import com.googlecode.objectify.test.LoadParentRefTests.TreeNode;
import com.googlecode.objectify.test.util.TestBase;
import com.googlecode.objectify.test.util.TestObjectify;

/**
 * Same as LoadParentRefTests but without the session clearing, so each load must reload some additional parts.
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class LoadParentRefTestsUsingSession extends TestBase
{
	/** */
	@Test
	public void testParentExists() throws Exception
	{
		fact.register(Father.class);
		fact.register(Child.class);
		
		TestObjectify ofy = fact.begin();
		
		Father f = new Father();
		f.foo = "foo";
		ofy.put(f);
		
		Child ch = new Child();
		ch.father = Ref.create(fact.<Father>getKey(f));
		ch.bar = "bar";
		ofy.put(ch);
		
		Ref<Child> fetchedRef = ofy.load().key(fact.<Child>getKey(ch));
		Child fetched = fetchedRef.get();
		
		assert fetched.bar.equals(ch.bar);
		assert fetched.father.get().id.equals(f.id);
		assert fetched.father.get().foo.equals(f.foo);
	}
	
	/** */
	@Test
	public void testTwoLevelsOfFetch() throws Exception
	{
		fact.register(TreeNode.class);
		
		TestObjectify ofy = fact.begin();
		
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
		assert fetched3.parent.get().id.equals(node2.id);
		assert fetched3.parent.get().foo.equals(node2.foo);
		assert fetched3.parent.get().parent.get().id.equals(node1.id);
		assert fetched3.parent.get().parent.get().foo.equals(node1.foo);
		assert fetched3.parent.get().parent.get().parent == null;
	}

	/** */
	@Test
	public void testMissingIntermediate() throws Exception
	{
		fact.register(TreeNode.class);
		
		TestObjectify ofy = fact.begin();
		
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
		
		assert fetched3.parent.get().id.equals(node2.id);
		assert fetched3.parent.get().foo == null;
		assert fetched3.parent.get().parent.get().id.equals(node1.id);
		assert fetched3.parent.get().parent.get().foo.equals(node1.foo);
		assert fetched3.parent.get().parent.get().parent == null;
	}

	/** */
	@Test
	public void testParentWithGroup() throws Exception
	{
		fact.register(Father.class);
		fact.register(ChildWithGroup.class);
		
		TestObjectify ofy = fact.begin();
		
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
		assert fetched.father.get() == null;

		// This should get a filled in ref
		Child fetched2 = ofy.load().group("group").key(fact.<Child>getKey(ch)).get();
		assert fetched2.father.get().id.equals(f.id);
		assert fetched2.father.get().foo.equals(f.foo);
	}
}