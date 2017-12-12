/*
 */

package com.googlecode.objectify.test;

import com.google.cloud.datastore.DatastoreException;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.impl.AsyncTransaction;
import com.googlecode.objectify.impl.ObjectifyImpl;
import com.googlecode.objectify.test.entity.Trivial;
import com.googlecode.objectify.test.util.TestBase;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static com.googlecode.objectify.ObjectifyService.factory;
import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * Tests using the deprecated transactionless() method. When the method is removed, this whole file should be removed.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
class TransactionTestsDeprecated extends TestBase {

	/**
	 * This should theoretically test the case where the cache is being modified even after a concurrency failure.
	 * However, it doesn't seem to trigger even without the logic fix in ListenableFuture.
	 */
	@Test
	void testConcurrencyFailure() throws Exception {
		factory().register(Trivial.class);

		final Trivial triv = new Trivial("foo", 5);
		final Key<Trivial> tk = ofy().save().entity(triv).now();

		try {
			ofy().transactNew(2, () -> {
				Trivial triv1 = ofy().transactionless().load().key(tk).now();
				Trivial triv2 = ofy().load().key(tk).now();

				triv1.setSomeString("bar");
				triv2.setSomeString("shouldn't work");

				ofy().transactionless().save().entity(triv1).now();
				ofy().save().entity(triv2).now();
			});
			assert false;	// must throw exception
		}
		catch (DatastoreException ex) {}

		Trivial fetched = ofy().load().key(tk).now();

		// This will be fetched from the cache, and must not be the "shouldn't work"
		assertThat(fetched.getSomeString()).isEqualTo("bar");
	}

	/** For transactionless tests */
	@Entity
	@Cache
	@Data
	@NoArgsConstructor
	private static class Thing {
		@Id long id;
		String foo;
		Thing(long id) { this.id = id; this.foo = "foo"; }
	}

	/** */
	@Test
	void testTransactionless() throws Exception {
		factory().register(Thing.class);

		for (int i=1; i<10; i++) {
			final Thing th = new Thing(i);
			ofy().save().entity(th).now();
		}

		ofy().transact(() -> {
			for (int i=1; i<10; i++)
				ofy().transactionless().load().type(Thing.class).id(i).now();

			ofy().save().entity(new Thing(99));
		});
	}

	/**
	 * This is a somewhat clunky way to test this, and requires making impl.getOptions() public,
	 * but it gets the job done.
	 */
	@Test
	void transactionalObjectifyInheritsCacheSetting() throws Exception {
		ofy().cache(false).transact(() -> {
			// Test in _and out_ of a transaction
			final ObjectifyImpl txnlessImpl = (ObjectifyImpl)ofy().transactionless();
			assertThat(txnlessImpl.getOptions().isCache()).isFalse();
		});
	}

	@Data
	private static class Counter {
		int counter = 0;
	}

	@Data
	private static class SimpleCommitListener implements Runnable {
		private boolean run = false;

		@Override
		public void run() {
			run = true;
		}

		boolean hasRun() {
			return run;
		}
	}

	@Data
	private static class CommitCountListener implements Runnable {
		private int commitCount = 0;

		@Override
		public void run() {
			commitCount++;
		}

		public int getCommitCount() {
			return commitCount;
		}
	}

	/**
	 */
	@Test
	void listenerNotCalledWithOrganicConcurrencyFailure() throws Exception {
		factory().register(Trivial.class);

		final Trivial triv = new Trivial("foo", 5);
		final Key<Trivial> tk = ofy().save().entity(triv).now();
		final SimpleCommitListener listener = new SimpleCommitListener();

		try {
			ofy().transactNew(1, () -> {
				final AsyncTransaction txn = ofy().getTransaction();
				txn.listenForCommit(listener);

				final Trivial triv1 = ofy().transactionless().load().key(tk).now();
				final Trivial triv2 = ofy().load().key(tk).now();

				ofy().transactionless().save().entity(triv1).now();
				ofy().save().entity(triv2).now();
			});
			assert false;	// must throw exception
		}
		catch (DatastoreException ex) {}

		assertThat(listener.hasRun()).isFalse();
	}

	/**
	 */
	@Test
	void listenerIsOnlyCalledOnceIfTransactionRetriesFromOrganicConcurrencyFailure() {
		factory().register(Trivial.class);

		final Trivial triv = new Trivial("foo", 5);
		final Key<Trivial> tk = ofy().save().entity(triv).now();
		final CommitCountListener listener = new CommitCountListener();
		final Counter counter = new Counter();

		ofy().transactNew(3, () -> {
			counter.counter++;
			final AsyncTransaction txn = ofy().getTransaction();
			txn.listenForCommit(listener);

			final Trivial triv1 = ofy().transactionless().load().key(tk).now();
			final Trivial triv2 = ofy().load().key(tk).now();

			if (counter.counter < 3) {
				ofy().transactionless().save().entity(triv1).now();
			}
			ofy().save().entity(triv2).now();
		});

		assertThat(counter.counter).isEqualTo(3);
		assertThat(listener.getCommitCount()).isEqualTo(1);
	}
}