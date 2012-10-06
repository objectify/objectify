/*
 */

package com.googlecode.objectify.test;

import java.util.Map;

import org.testng.annotations.Test;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Load;
import com.googlecode.objectify.test.util.TestBase;
import com.googlecode.objectify.test.util.TestObjectify;

/**
 * Really simple reduced tests of @Load
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class LoadFieldTestsReallySimple extends TestBase
{
	/** */
	@Entity
	public static class Father {
		public @Id long id;
		public @Load Child child;

		@Override
		public String toString() {
			return this.getClass().getSimpleName() + "(" + id + ", " + child + ")";
		}
	}

	/** */
	@Entity
	public static class Child {
		public @Id long id;

		@Override
		public String toString() {
			return this.getClass().getSimpleName() + "(" + id + ")";
		}
	}

	/** */
	@Test
	public void testLoadChildField() throws Exception
	{
		fact.register(Father.class);
		fact.register(Child.class);

		TestObjectify ofy = fact.begin();

		Child ch = new Child();
		ch.id = 123;
		ofy.put(ch);

		Father f = new Father();
		f.id = 456;
		f.child = ch;
		Key<Father> kf = ofy.put(f);

		ofy.clear();

		Ref<Father> ref = ofy.load().key(kf);
		Father fetched = ref.get();

		assert fetched.child.id == ch.id;
	}

	/** */
	@Test
	public void testLoadBoth() throws Exception
	{
		fact.register(Father.class);
		fact.register(Child.class);

		TestObjectify ofy = fact.begin();

		Child ch = new Child();
		ch.id = 123;
		Key<Child> kch = ofy.put(ch);

		Father f = new Father();
		f.id = 456;
		f.child = ch;
		Key<Father> kf = ofy.put(f);

		ofy.clear();

		@SuppressWarnings({ "unchecked", "rawtypes" })	// erasure is retarded
		Map<Key<Object>, Object> result = ofy.load().keys((Key)kch, (Key)kf);
		Father fetched = (Father)result.get(kf);

		assert fetched.child.id == ch.id;
	}

// Conditions on @Load for entities are no longer allowed
//	/** */
//	@Entity
//	public static class ChildLoadGroup {
//		public @Id long id;
//		public String bar;
//
//		@Override
//		public String toString() {
//			return this.getClass().getSimpleName() + "(" + id + ", " + bar + ")";
//		}
//	}
//
//	/** */
//	@Entity
//	public static class FatherLoadGroup {
//		public static class Yes {}
//
//		public @Id long id;
//		public @Load(Yes.class) ChildLoadGroup child;
//
//		@Override
//		public String toString() {
//			return this.getClass().getSimpleName() + "(" + id + ", " + child + ")";
//		}
//	}
//
//	/** */
//	@Test
//	public void testLoadChildFieldWithGroup() throws Exception
//	{
//		fact.register(FatherLoadGroup.class);
//		fact.register(ChildLoadGroup.class);
//
//		TestObjectify ofy = fact.begin();
//
//		ChildLoadGroup ch = new ChildLoadGroup();
//		ch.id = 123;
//		ch.bar = "barvalue";
//		ofy.put(ch);
//
//		FatherLoadGroup f = new FatherLoadGroup();
//		f.id = 456;
//		f.child = ch;
//		Key<FatherLoadGroup> kf = ofy.put(f);
//
//		Ref<FatherLoadGroup> ref;
//		FatherLoadGroup fetched;
//
//		ofy.clear();
//		ref = ofy.load().key(kf);
//		fetched = ref.get();
//		assert fetched.child.id == ch.id;
//		assert fetched.child.bar == null;
//
//		ofy.clear();
//		ref = ofy.load().group(Yes.class).key(kf);
//		fetched = ref.get();
//		assert fetched.child.id == ch.id;
//		assert fetched.child.bar.equals(ch.bar);
//	}
//
//	/** */
//	@Test
//	public void testReloadChildFieldWithGroupFromSession() throws Exception
//	{
//		fact.register(FatherLoadGroup.class);
//		fact.register(ChildLoadGroup.class);
//
//		TestObjectify ofy = fact.begin();
//
//		ChildLoadGroup ch = new ChildLoadGroup();
//		ch.id = 123;
//		ch.bar = "barvalue";
//		ofy.put(ch);
//
//		FatherLoadGroup f = new FatherLoadGroup();
//		f.id = 456;
//		f.child = ch;
//		Key<FatherLoadGroup> kf = ofy.put(f);
//
//		Ref<FatherLoadGroup> ref;
//		FatherLoadGroup fetched;
//
//		ofy.clear();
//		ref = ofy.load().key(kf);
//
//		// Now load again without clear
//		ref = ofy.load().group(Yes.class).key(kf);
//		fetched = ref.get();
//		assert fetched.child.id == ch.id;
//		assert fetched.child.bar.equals(ch.bar);
//	}
}