/*
 */

package com.googlecode.objectify.test;

import org.testng.annotations.Test;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Parent;
import com.googlecode.objectify.test.util.TestBase;

import static com.googlecode.objectify.test.util.TestObjectifyService.fact;
import static com.googlecode.objectify.test.util.TestObjectifyService.ofy;

/**
 * Tests the fetching system for simple parent values.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class ParentTests extends TestBase
{
	/** */
	@Entity
	public static class Father {
		public @Id Long id;
		public String foo;

		public Father() {}
		public Father(long id) { this.id = id; }

		@Override public boolean equals(Object obj) {
			return ((Father)obj).id.equals(id);
		}
	}

	/** */
	@Entity
	public static class KeyChild {
		public @Id Long id;
		public @Parent Key<Father> father;
		public String bar;
	}

	/** */
	@Entity
	public static class RefChild {
		public @Id Long id;
		public @Parent Ref<Father> father;
		public String bar;
	}

	/** */
	@Test
	public void testKeyParent() throws Exception
	{
		fact().register(Father.class);
		fact().register(KeyChild.class);

		KeyChild ch = new KeyChild();
		ch.father = Key.create(Father.class, 123);
		ch.bar = "bar";
		ofy().put(ch);

		KeyChild fetched = ofy().get(Key.create(ch));

		assert fetched.bar.equals(ch.bar);
		assert fetched.father.equals(ch.father);
	}

	/** */
	@Test
	public void testRefParent() throws Exception
	{
		fact().register(Father.class);
		fact().register(RefChild.class);

		RefChild ch = new RefChild();
		ch.father = Ref.create(Key.create(Father.class, 123));
		ch.bar = "bar";
		ofy().put(ch);

		RefChild fetched = ofy().get(Key.create(ch));

		assert fetched.bar.equals(ch.bar);
		assert fetched.father.equals(ch.father);
	}
}