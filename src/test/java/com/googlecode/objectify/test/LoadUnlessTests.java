/*
 */

package com.googlecode.objectify.test;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Load;
import com.googlecode.objectify.test.LoadUnlessTests.One.No;
import com.googlecode.objectify.test.LoadUnlessTests.One.Yes;
import com.googlecode.objectify.test.util.TestBase;
import org.testng.annotations.Test;
import java.util.ArrayList;
import java.util.List;
import static com.googlecode.objectify.test.util.TestObjectifyService.fact;
import static com.googlecode.objectify.test.util.TestObjectifyService.ofy;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

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
	public void testLoadUnless() throws Exception {
		fact().register(One.class);
		fact().register(Two.class);

		Two twoAlways = new Two();
		twoAlways.id = 456;
		Key<Two> twoAlwaysKey = ofy().save().entity(twoAlways).now();
		Ref<Two> twoAlwaysRef = Ref.create(twoAlwaysKey);

		Two twoSometimes = new Two();
		twoSometimes.id = 789;
		Key<Two> twoSometimesKey = ofy().save().entity(twoSometimes).now();
		Ref<Two> twoSometimesRef = Ref.create(twoSometimesKey);

		One one = new One();
		one.id = 123;
		one.always = twoAlwaysRef;
		one.sometimes = twoSometimesRef;
		ofy().save().entity(one).now();

		ofy().clear();
		One fetchedDefault = ofy().load().entity(one).now();
		assert fetchedDefault.always.isLoaded();
		assert !fetchedDefault.sometimes.isLoaded();

		ofy().clear();
		One fetchedYes = ofy().load().group(Yes.class).entity(one).now();
		assert fetchedYes.always.isLoaded();
		assert fetchedYes.sometimes.isLoaded();

		ofy().clear();
		One fetchedNo = ofy().load().group(No.class).entity(one).now();
		assert !fetchedNo.always.isLoaded();
		assert !fetchedNo.sometimes.isLoaded();

		ofy().clear();
		One fetchedYesNo = ofy().load().group(Yes.class, No.class).entity(one).now();
		assert !fetchedYesNo.always.isLoaded();
		assert !fetchedYesNo.sometimes.isLoaded();
	}

	/** */
	@Entity
	public static class Multi {
		public @Id long id;
		public @Load(unless=No.class) List<Ref<Two>> always = new ArrayList<>();
	}

	/** */
	@Test
	public void testLoadUnlessMulti() throws Exception {
		fact().register(Multi.class);
		fact().register(Two.class);

		Two two1 = new Two();
		two1.id = 456;
		Key<Two> two1Key = ofy().save().entity(two1).now();
		Ref<Two> two1Ref = Ref.create(two1Key);

		Two two2 = new Two();
		two2.id = 789;
		Key<Two> two2Key = ofy().save().entity(two2).now();
		Ref<Two> two2Ref = Ref.create(two2Key);

		Multi multi = new Multi();
		multi.id = 123;
		multi.always.add(two1Ref);
		multi.always.add(two2Ref);
		ofy().save().entity(multi).now();

		ofy().clear();
		Multi fetchedDefault = ofy().load().entity(multi).now();
		assertThat(fetchedDefault.always, hasSize(2));
		for (Ref<Two> ref : fetchedDefault.always) {
			assertThat(ref.isLoaded(), equalTo(true));
		}

		ofy().clear();
		Multi fetchedNo = ofy().load().group(No.class).entity(multi).now();
		assertThat(fetchedNo.always, hasSize(2));
		for (Ref<Two> ref : fetchedNo.always) {
			assertThat(ref.isLoaded(), equalTo(false));
		}
	}
}
