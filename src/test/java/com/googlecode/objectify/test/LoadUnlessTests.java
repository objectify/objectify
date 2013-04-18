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

import static com.googlecode.objectify.test.util.TestObjectifyService.fact;
import static com.googlecode.objectify.test.util.TestObjectifyService.ofy;

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
		fact().register(One.class);
		fact().register(Two.class);

		Two twoAlways = new Two();
		twoAlways.id = 456;
		Key<Two> twoAlwaysKey = ofy().put(twoAlways);
		Ref<Two> twoAlwaysRef = Ref.create(twoAlwaysKey);

		Two twoSometimes = new Two();
		twoSometimes.id = 789;
		Key<Two> twoSometimesKey = ofy().put(twoSometimes);
		Ref<Two> twoSometimesRef = Ref.create(twoSometimesKey);

		One one = new One();
		one.id = 123;
		one.always = twoAlwaysRef;
		one.sometimes = twoSometimesRef;
		ofy().put(one);

		ofy().clear();
		One fetchedDefault = ofy().load().entity(one).get();
		assert fetchedDefault.always.isLoaded();
		assert !fetchedDefault.sometimes.isLoaded();

		ofy().clear();
		One fetchedYes = ofy().load().group(Yes.class).entity(one).get();
		assert fetchedYes.always.isLoaded();
		assert fetchedYes.sometimes.isLoaded();

		ofy().clear();
		One fetchedNo = ofy().load().group(No.class).entity(one).get();
		assert !fetchedNo.always.isLoaded();
		assert !fetchedNo.sometimes.isLoaded();

		ofy().clear();
		One fetchedYesNo = ofy().load().group(Yes.class, No.class).entity(one).get();
		assert !fetchedYesNo.always.isLoaded();
		assert !fetchedYesNo.sometimes.isLoaded();
	}
}