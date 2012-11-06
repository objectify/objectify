/*
 */

package com.googlecode.objectify.test;

import org.testng.annotations.Test;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Load;
import com.googlecode.objectify.test.LoadUnlessTests.One.No;
import com.googlecode.objectify.test.LoadUnlessTests.One.Yes;
import com.googlecode.objectify.test.util.TestBase;
import com.googlecode.objectify.test.util.TestObjectify;

/**
 * Tests of @Load(unless=Blah.class)
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class LoadUnlessTests extends TestBase
{
	/** */
	@Entity
	public static class One {
		public static class Yes {}
		public static class No {}

		public @Id long id;
		public @Load(unless=No.class) Ref<Two> always;
		public @Load(value=Yes.class, unless=No.class) Ref<Two> sometimes;
	}

	/** */
	@Entity
	public static class Two {
		public @Id long id;
	}

	/** */
	@Test
	public void testLoadUnless() throws Exception
	{
		fact.register(One.class);
		fact.register(Two.class);

		TestObjectify ofy = fact.begin();

		Two two = new Two();
		two.id = 456;
		Key<Two> twoKey = ofy.put(two);
		Ref<Two> twoRef = Ref.create(twoKey);

		One one = new One();
		one.id = 123;
		one.always = twoRef;
		one.sometimes = twoRef;
		ofy.put(one);

		ofy.clear();
		One fetchedDefault = ofy.load().entity(one).get();
		assert fetchedDefault.always.getValue() != null;
		assert fetchedDefault.sometimes.getValue() == null;

		ofy.clear();
		One fetchedYes = ofy.load().group(Yes.class).entity(one).get();
		assert fetchedYes.always.getValue() != null;
		assert fetchedYes.sometimes.getValue() != null;

		ofy.clear();
		One fetchedNo = ofy.load().group(No.class).entity(one).get();
		assert fetchedNo.always.getValue() == null;
		assert fetchedNo.sometimes.getValue() == null;

		ofy.clear();
		One fetchedYesNo = ofy.load().group(Yes.class, No.class).entity(one).get();
		assert fetchedYesNo.always.getValue() == null;
		assert fetchedYesNo.sometimes.getValue() == null;
	}
}