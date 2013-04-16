/*
 */

package com.googlecode.objectify.test;

import org.testng.annotations.Test;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Load;
import com.googlecode.objectify.test.LoadGroupInheritance.Father.Bottom;
import com.googlecode.objectify.test.LoadGroupInheritance.Father.Middle;
import com.googlecode.objectify.test.LoadGroupInheritance.Father.Top;
import com.googlecode.objectify.test.util.TestBase;
import com.googlecode.objectify.test.util.TestObjectify;

/**
 * Tests the inheritance of load group classes
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class LoadGroupInheritance extends TestBase
{
	/** */
	@Entity
	public static class Father {
		public static class Bottom {}
		public static class Middle extends Bottom {}
		public static class Top extends Middle {}

		public @Id long id;
		public @Load(Middle.class) Ref<Child> child;

		public Father() {}
		public Father(long id, Ref<Child> ch) {
			this.id = id;
			this.child = ch;
		}

		@Override
		public String toString() {
			return this.getClass().getSimpleName() + "(" + id + ", " + child + ")";
		}
	}

	/** */
	@Entity
	public static class Child {
		public @Id long id;

		public Child() {}
		public Child(long id) { this.id = id; }

		@Override
		public String toString() {
			return this.getClass().getSimpleName() + "(" + id + ")";
		}
	}

	/** */
	@Test
	public void testLoadNoGroup() throws Exception
	{
		fact.register(Father.class);
		fact.register(Child.class);

		TestObjectify ofy = fact.begin();

		Child ch = new Child(123);
		Key<Child> kch = ofy.put(ch);

		Father f = new Father(456, Ref.create(kch));
		Key<Father> kf = ofy.put(f);

		ofy.clear();
		Ref<Father> fatherRef = ofy.load().key(kf);
		assert !fatherRef.get().child.isLoaded();
	}

	/** */
	@Test
	public void testLoadBottomGroup() throws Exception
	{
		fact.register(Father.class);
		fact.register(Child.class);

		TestObjectify ofy = fact.begin();

		Child ch = new Child(123);
		Key<Child> kch = ofy.put(ch);

		Father f = new Father(456, Ref.create(kch));
		Key<Father> kf = ofy.put(f);

		ofy.clear();
		Ref<Father> fatherRef = ofy.load().group(Bottom.class).key(kf);
		assert !fatherRef.get().child.isLoaded();
	}

	/** */
	@Test
	public void testLoadMiddleGroup() throws Exception
	{
		fact.register(Father.class);
		fact.register(Child.class);

		TestObjectify ofy = fact.begin();

		Child ch = new Child(123);
		Key<Child> kch = ofy.put(ch);

		Father f = new Father(456, Ref.create(kch));
		Key<Father> kf = ofy.put(f);

		ofy.clear();
		Ref<Father> fatherRef = ofy.load().group(Middle.class).key(kf);
		assert fatherRef.get().child.get() != null;
	}

	/** */
	@Test
	public void testLoadTopGroup() throws Exception
	{
		fact.register(Father.class);
		fact.register(Child.class);

		TestObjectify ofy = fact.begin();

		Child ch = new Child(123);
		Key<Child> kch = ofy.put(ch);

		Father f = new Father(456, Ref.create(kch));
		Key<Father> kf = ofy.put(f);

		ofy.clear();
		Ref<Father> fatherRef = ofy.load().group(Top.class).key(kf);
		assert fatherRef.get().child.get() != null;
	}
}