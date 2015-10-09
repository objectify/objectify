/*
 */

package com.googlecode.objectify.test;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.LoadResult;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Load;
import com.googlecode.objectify.annotation.Parent;
import com.googlecode.objectify.test.LoadParentRefTests.ChildWithGroup.Group;
import com.googlecode.objectify.test.util.TestBase;
import org.testng.annotations.Test;

import static com.googlecode.objectify.test.util.TestObjectifyService.fact;
import static com.googlecode.objectify.test.util.TestObjectifyService.ofy;

/**
 * Tests the fetching system for parent values using Ref<?> holders.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class LoadParentRefTests extends TestBase
{
	/** */
	@Entity
	public static class Father {
		public @Id Long id;
		public String foo;
	}

	/** */
	@Entity
	public static class Child {
		public @Id Long id;
		public @Load @Parent Ref<Father> father;
		public String bar;
	}

	/** Clears the session to get a full load */
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
		ofy().save().entity(ch).now();

		ofy().clear();

		LoadResult<Child> fetchedRef = ofy().load().key(Key.create(ch));
		Child fetched = fetchedRef.now();

		assert fetched.bar.equals(ch.bar);
		assert fetched.father.get().id.equals(f.id);
		assert fetched.father.get().foo.equals(f.foo);
	}

	/** */
	@Entity
	public static class TreeNode {
		public @Id Long id;
		public @Load @Parent Ref<TreeNode> parent;
		public String foo;
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

		ofy().clear();

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

		ofy().clear();

		// Doing this step by step to make it easier for debugging
		LoadResult<TreeNode> fetched3Ref = ofy().load().key(key3);
		TreeNode fetched3 = fetched3Ref.now();

		assert fetched3.parent.get() == null;
		assert fetched3.parent.key().equals(key2);
		assert fetched3.parent.key().getParent().equals(key1);
	}

	/** */
	@Entity
	public static class ChildWithGroup {
		public static class Group {}

		public @Id Long id;
		public @Load(Group.class) @Parent Ref<Father> father;
		public String bar;
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
		Key<ChildWithGroup> kch = ofy().save().entity(ch).now();

		// This should get an empty ref
		ofy().clear();
		ChildWithGroup fetched = ofy().load().key(kch).now();
		assert fetched.father.key().getId() == f.id;
		assert !fetched.father.isLoaded();

		// Upgrade in the same session
		ChildWithGroup fetched2 = ofy().load().group(Group.class).key(kch).now();
		assert fetched2 == fetched;
		assert fetched2.father.isLoaded();
		assert fetched2.father.get().id.equals(f.id);
		assert fetched2.father.get().foo.equals(f.foo);

		// Also should work after session is cleared, but objects will not be same
		ofy().clear();
		ChildWithGroup fetched3 = ofy().load().group(Group.class).key(kch).now();
		assert fetched3 != fetched2;
		assert fetched3.father.isLoaded();
		assert fetched3.father.get().id.equals(f.id);
		assert fetched3.father.get().foo.equals(f.foo);
	}
}
