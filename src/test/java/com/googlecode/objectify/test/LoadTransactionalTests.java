/*
 */

package com.googlecode.objectify.test;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.VoidWork;
import com.googlecode.objectify.Work;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Load;
import com.googlecode.objectify.test.LoadTransactionalTests.One.Bar;
import com.googlecode.objectify.test.LoadTransactionalTests.One.Foo;
import com.googlecode.objectify.test.LoadUnlessTests.One.No;
import com.googlecode.objectify.test.util.TestBase;
import org.testng.annotations.BeforeMethod;
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

	private One one;
	private Key<Two> twoWithGroupKey;

	@Override
	@BeforeMethod
	public void setUp() {
		super.setUp();

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
		twoWithGroupKey = ofy().save().entity(twoWithGroup).now();
		final Ref<Two> twoWithGroupRef = Ref.create(twoWithGroupKey);

		one = new One();
		one.id = 123;
		one.always = twoAlwaysRef;
		one.withUnless = twoWithUnlessRef;
		one.withGroup = twoWithGroupRef;
		ofy().save().entity(one).now();

		// clear the session cache so we can test whether and when refs were dereferenced
		ofy().clear();
	}

	/** */
	@Test
	public void properLoadBehaviorInTransactions() throws Exception {
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

	@Test
	public void testInitializeRefInTransactionDerefinTransactionless() {
		// Initialize the ref in transaction context but don't deref until we're back in the transacitonless
		// context. The entity pointed to by the ref won't be loaded in either session. Using withGroup
		// as it won't be automatically loaded.
		One fetched = ofy().transact(new Work<One>() {
			@Override
			public One run() {
				One fetched = ofy().load().entity(one).now();
				assert !ofy().isLoaded(twoWithGroupKey);
				return fetched;
			}
		});

		assert !ofy().isLoaded(twoWithGroupKey);

		// Now deref, inside the transactionless contact. Since the Ref is bound to the Objectify instance,
		// the load happens in the current context. So the entity is loaded in the transactionless session and
		// not loaded in the transaction session (which is since gone):
		fetched.withGroup.get();

		assert ofy().isLoaded(twoWithGroupKey);
	}

	@Test
	public void testInitializeRefInTransactionlessDerefInTransaction() {
		ofy().transact(new VoidWork() {
			@Override
			public void vrun() {
				// Initialize the ref in transactionless context but don't deref until we're back in the transaciton
				// context. The entity pointed to by the ref won't be loaded in either session. Using withGroup
				// as it won't be automatically loaded.
				One fetched = ofy().transactionless(new Work<One>() {
					@Override
					public One run() {
						One fetched = ofy().load().entity(one).now();
						assert !ofy().isLoaded(twoWithGroupKey);
						return fetched;
					}
				});

				assert !ofy().isLoaded(twoWithGroupKey);

				// Now deref, inside the transaction contact. Since the Ref is bound to the transactionless instance,
				// the load happens in the transactionless context and not loaded in the transaction session:
				fetched.withGroup.get();

				ofy().transactionless(new VoidWork() {
					@Override
					public void vrun() {
						assert ofy().isLoaded(twoWithGroupKey);
					}
				});
				assert !ofy().isLoaded(twoWithGroupKey);
			}
		});
	}
}