/*
 */

package com.googlecode.objectify.test;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.LoadResult;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Load;
import com.googlecode.objectify.test.LoadGroupInheritance.Father.Bottom;
import com.googlecode.objectify.test.LoadGroupInheritance.Father.Middle;
import com.googlecode.objectify.test.LoadGroupInheritance.Father.Top;
import com.googlecode.objectify.test.util.TestBase;
import org.testng.annotations.Test;

import static com.googlecode.objectify.test.util.TestObjectifyService.fact;
import static com.googlecode.objectify.test.util.TestObjectifyService.ofy;

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
		fact().register(Father.class);
		fact().register(Child.class);

		Child ch = new Child(123);
		Key<Child> kch = ofy().save().entity(ch).now();

		Father f = new Father(456, Ref.create(kch));
		Key<Father> kf = ofy().save().entity(f).now();

		ofy().clear();
		LoadResult<Father> fatherRef = ofy().load().key(kf);
		assert !fatherRef.now().child.isLoaded();
	}

	/** */
	@Test
	public void testLoadBottomGroup() throws Exception
	{
		fact().register(Father.class);
		fact().register(Child.class);

		Child ch = new Child(123);
		Key<Child> kch = ofy().save().entity(ch).now();

		Father f = new Father(456, Ref.create(kch));
		Key<Father> kf = ofy().save().entity(f).now();

		ofy().clear();
		LoadResult<Father> fatherRef = ofy().load().group(Bottom.class).key(kf);
		assert !fatherRef.now().child.isLoaded();
	}

	/** */
	@Test
	public void testLoadMiddleGroup() throws Exception
	{
		fact().register(Father.class);
		fact().register(Child.class);

		Child ch = new Child(123);
		Key<Child> kch = ofy().save().entity(ch).now();

		Father f = new Father(456, Ref.create(kch));
		Key<Father> kf = ofy().save().entity(f).now();

		ofy().clear();
		LoadResult<Father> fatherRef = ofy().load().group(Middle.class).key(kf);
		assert fatherRef.now().child.get() != null;
	}

	/** */
	@Test
	public void testLoadTopGroup() throws Exception
	{
		fact().register(Father.class);
		fact().register(Child.class);

		Child ch = new Child(123);
		Key<Child> kch = ofy().save().entity(ch).now();

		Father f = new Father(456, Ref.create(kch));
		Key<Father> kf = ofy().save().entity(f).now();

		ofy().clear();
		LoadResult<Father> fatherRef = ofy().load().group(Top.class).key(kf);
		assert fatherRef.now().child.get() != null;
	}
}