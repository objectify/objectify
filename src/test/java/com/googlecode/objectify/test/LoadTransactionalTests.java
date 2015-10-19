/*
 */

package com.googlecode.objectify.test;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.VoidWork;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Load;
import com.googlecode.objectify.test.LoadTransactionalTests.One.Bar;
import com.googlecode.objectify.test.LoadTransactionalTests.One.Foo;
import com.googlecode.objectify.test.LoadUnlessTests.One.No;
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
 * Tests of @Load annotation in transactions
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class LoadTransactionalTests extends TestBase
{
	/** */
	@Entity
	public static class One {
		public static class Foo {}
		public static class Bar {}

		public @Id long id;
		public @Load Ref<Two> always;
		public @Load(unless=Foo.class) Ref<Two> withUnless;
		public @Load(Foo.class) Ref<Two> withGroup;
	}

	/** */
	@Entity
	public static class Two {
		public @Id long id;
	}

	/** */
	@Test
	public void properLoadBehaviorInTransactions() throws Exception {
		fact().register(One.class);
		fact().register(Two.class);

		final Two twoAlways = new Two();
		twoAlways.id = 123;
		final Key<Two> twoAlwaysKey = ofy().save().entity(twoAlways).now();
		final Ref<Two> twoAlwaysRef = Ref.create(twoAlwaysKey);

		final Two twoWithUnless = new Two();
		twoWithUnless.id = 456;
		final Key<Two> twoWithUnlessKey = ofy().save().entity(twoWithUnless).now();
		final Ref<Two> twoWithUnlessRef = Ref.create(twoWithUnlessKey);

		final Two twoWithGroup = new Two();
		twoWithGroup.id = 789;
		final Key<Two> twoWithGroupKey = ofy().save().entity(twoWithGroup).now();
		final Ref<Two> twoWithGroupRef = Ref.create(twoWithGroupKey);

		final One one = new One();
		one.id = 123;
		one.always = twoAlwaysRef;
		one.withUnless = twoWithUnlessRef;
		one.withGroup = twoWithGroupRef;
		ofy().save().entity(one).now();

		ofy().transact(new VoidWork() {
			@Override
			public void vrun() {
				final One fetched = ofy().load().entity(one).now();
				assert !fetched.always.isLoaded();
				assert !fetched.withUnless.isLoaded();
				assert !fetched.withGroup.isLoaded();
			}
		});

		ofy().transact(new VoidWork() {
			@Override
			public void vrun() {
				final One fetched = ofy().load().group(Foo.class).entity(one).now();
				assert !fetched.always.isLoaded();
				assert !fetched.withUnless.isLoaded();
				assert fetched.withGroup.isLoaded();
			}
		});

		ofy().transact(new VoidWork() {
			@Override
			public void vrun() {
				final One fetched = ofy().load().group(Bar.class).entity(one).now();
				assert !fetched.always.isLoaded();
				assert !fetched.withUnless.isLoaded();
				assert !fetched.withGroup.isLoaded();
			}
		});
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