/*
 */

package com.googlecode.objectify.test;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.LoadResult;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.test.LoadParentRefTests.Child;
import com.googlecode.objectify.test.LoadParentRefTests.ChildWithGroup;
import com.googlecode.objectify.test.LoadParentRefTests.ChildWithGroup.Group;
import com.googlecode.objectify.test.LoadParentRefTests.Father;
import com.googlecode.objectify.test.LoadParentRefTests.TreeNode;
import com.googlecode.objectify.test.util.TestBase;
import org.testng.annotations.Test;

import static com.googlecode.objectify.test.util.TestObjectifyService.fact;
import static com.googlecode.objectify.test.util.TestObjectifyService.ofy;

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
		fact().register(Father.class);
		fact().register(Child.class);

		Father f = new Father();
		f.foo = "foo";
		ofy().save().entity(f).now();

		Child ch = new Child();
		ch.father = Ref.create(Key.create(f));
		ch.bar = "bar";
		Key<Child> kch = ofy().save().entity(ch).now();

		LoadResult<Child> fetchedRef = ofy().load().key(kch);
		Child fetched = fetchedRef.now();

		assert fetched.bar.equals(ch.bar);
		assert fetched.father.get().id.equals(f.id);
		assert fetched.father.get().foo.equals(f.foo);
	}

	/** */
	@Test
	public void testTwoLevelsOfFetch() throws Exception
	{
		fact().register(TreeNode.class);

		TreeNode node1 = new TreeNode();
		node1.foo = "foo1";
		ofy().save().entity(node1).now();

		TreeNode node2 = new TreeNode();
		node2.parent = Ref.create(node1);
		node2.foo = "foo2";
		ofy().save().entity(node2).now();

		TreeNode node3 = new TreeNode();
		node3.parent = Ref.create(node2);
		node3.foo = "foo3";
		ofy().save().entity(node3).now();

		TreeNode fetched3 = ofy().load().entity(node3).now();

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
		fact().register(TreeNode.class);

		TreeNode node1 = new TreeNode();
		node1.foo = "foo1";
		Key<TreeNode> key1 = ofy().save().entity(node1).now();

		// Node2 should not exist but should have a concrete id for node3
		TreeNode node2 = new TreeNode();
		node2.id = 999L;
		node2.parent = Ref.create(key1);
		Key<TreeNode> key2 = Key.create(node2);

		TreeNode node3 = new TreeNode();
		node3.parent = Ref.create(key2);
		node3.foo = "foo3";
		Key<TreeNode> key3 = ofy().save().entity(node3).now();

		// Doing this step by step to make it easier for debugging
		LoadResult<TreeNode> fetched3Ref = ofy().load().key(key3);
		TreeNode fetched3 = fetched3Ref.now();

		assert fetched3.parent.get() == null;
		assert fetched3.parent.key().equals(key2);
		assert fetched3.parent.key().getParent().equals(key1);
	}

	/** */
	@Test
	public void testParentWithGroup() throws Exception
	{
		fact().register(Father.class);
		fact().register(ChildWithGroup.class);

		Father f = new Father();
		f.foo = "foo";
		ofy().save().entity(f).now();

		ChildWithGroup ch = new ChildWithGroup();
		ch.father = Ref.create(Key.create(f));
		ch.bar = "bar";
		ofy().save().entity(ch).now();

		ofy().clear();

		// This should get an uninitialized ref
		ChildWithGroup fetched = ofy().load().entity(ch).now();
		assert fetched.father.key().getId() == f.id;
		assert !fetched.father.isLoaded();

		// This should get a filled in ref
		ChildWithGroup fetched2 = ofy().load().group(Group.class).key(Key.create(ch)).now();
		fetched2.father.get();
		assert fetched2.father.get().id.equals(f.id);
		assert fetched2.father.get().foo.equals(f.foo);
	}
}