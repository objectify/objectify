/*
 */

package com.googlecode.objectify.test;

import com.google.cloud.datastore.DatastoreException;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.TxnType;
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

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static com.googlecode.objectify.ObjectifyService.factory;
import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * Tests of transactional behavior.  Since many transactional characteristics are
 * determined by race conditions and other realtime effects, these tests are not
 * very thorough.  We will assume that Google's transactions work.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
class TransactionTests extends TestBase {

	/** */
	@Test
	void exerciseSimpleTransactions() throws Exception {
		factory().register(Trivial.class);

		final Trivial triv = new Trivial("foo", 5);

		final Key<Trivial> k = ofy().transact(() -> ofy().save().entity(triv).now());

		final Trivial fetched = ofy().load().key(k).now();

		assertThat(fetched).isEqualTo(triv);
	}

	/** */
	@Entity
	@Cache
	@Data
	private static class HasSimpleCollection {
		@Id Long id;
		List<String> stuff = new ArrayList<>();
	}

	/** */
	@Test
	void testInAndOutOfTransaction() throws Exception {
		factory().register(HasSimpleCollection.class);

		final HasSimpleCollection simple = new HasSimpleCollection();
		ofy().save().entity(simple).now();

		final HasSimpleCollection simple2 = ofy().transact(() -> {
			final HasSimpleCollection simple21 = ofy().load().type(HasSimpleCollection.class).id(simple.id).now();
			simple21.stuff.add("blah");
			ofy().save().entity(simple21);
			return simple21;
		});

		ofy().clear();
		final HasSimpleCollection simple3 = ofy().load().type(HasSimpleCollection.class).id(simple.id).now();

		assertThat(simple2.stuff).isEqualTo(simple3.stuff);
	}

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
				Trivial triv1 = ofy().transactionless(() -> ofy().load().key(tk).now());
				Trivial triv2 = ofy().load().key(tk).now();

				triv1.setSomeString("bar");
				triv2.setSomeString("shouldn't work");

				ofy().transactionless(() -> ofy().save().entity(triv1).now());
				ofy().save().entity(triv2).now();
			});
			assert false;	// must throw exception
		}
		catch (DatastoreException ex) {}

		final Trivial fetched = ofy().load().key(tk).now();

		// This will be fetched from the cache, and must not be the "shouldn't work"
		assertThat(fetched.getSomeString()).isEqualTo("bar");
	}

	/**
	 */
	@Test
	void testTransactWork() throws Exception {
		factory().register(Trivial.class);

		final Trivial triv = new Trivial("foo", 5);
		ofy().save().entity(triv).now();

		final Trivial updated = ofy().transact(() -> {
			Trivial result = ofy().load().entity(triv).now();
			result.setSomeNumber(6);
			ofy().save().entity(result);
			return result;
		});
		assertThat(updated.getSomeNumber()).isEqualTo(6);

		final Trivial fetched = ofy().load().entity(triv).now();
		assertThat(fetched.getSomeNumber()).isEqualTo(6);
	}

	/**
	 * Make sure that an async delete in a transaction fixes the session cache when the transaction is committed.
	 */
	@Test
	void testAsyncDelete() throws Exception {
		factory().register(Trivial.class);

		final Trivial triv = new Trivial("foo", 5);

		// Make sure it's in the session (and memcache for that matter)
		saveClearLoad(triv);

		ofy().transact(() -> {
			// Load this, enlist in txn
			final Trivial fetched = ofy().load().entity(triv).now();

			// Do this async, don't complete it manually
			ofy().delete().entity(fetched);
		});

		assertThat(ofy().load().entity(triv).now()).isNull();
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
			for (int i=1; i<10; i++) {
				final int index = i;
				ofy().transactionless(() -> ofy().load().type(Thing.class).id(index).now());
			}

			ofy().save().entity(new Thing(99));
		});
	}

	/**
	 */
	@Test
	void testTransactionRollback() throws Exception {
		factory().register(Trivial.class);

		try {
			ofy().transact(() -> {
				final Trivial triv = new Trivial("foo", 5);
				ofy().save().entity(triv).now();
				throw new RuntimeException();
			});
		} catch (RuntimeException ex) {}

		// Now verify that it was not saved
		final Trivial fetched = ofy().load().type(Trivial.class).first().now();
		assertThat(fetched).isNull();
	}

	/**
	 */
	@Test
	void transactionalObjectifyInheritsCacheSetting() throws Exception {
		ofy().cache(false).transact(() -> {
			// Test in _and out_ of a transaction
			assertThat(((ObjectifyImpl)ofy()).getOptions().isCache()).isFalse();

			ofy().transactionless(() -> {
				assertThat(((ObjectifyImpl)ofy()).getOptions().isCache()).isFalse();
			});
		});
	}
	
	/**
	 */
	@Test
	void executeMethodWorks() throws Exception {
		ofy().execute(TxnType.REQUIRED, () -> {
			assertThat(ofy().load().type(Trivial.class).id(123L).now()).isNull();
		});
	}

	@Data
	private static class Counter {
		int counter = 0;
	}

	/**
	 */
	@Test
	void limitsTries() throws Exception {
		final Counter counter = new Counter();

		try {
			ofy().transactNew(3, () -> {
				counter.counter++;
				throw new DatastoreException(10, "too much contention on these datastore entities", "ABORTED");
			});
		} catch (DatastoreException e) {}

		assertThat(counter.counter).isEqualTo(3);
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

	/**
	 */
	@Test
	void transactionListeners() {
		final SimpleCommitListener listener = new SimpleCommitListener();

		ofy().transact(() -> {
			final AsyncTransaction txn = ofy().getTransaction();
			txn.listenForCommit(listener);

			assertThat(listener.hasRun()).isFalse();
		});

		assertThat(listener.hasRun()).isTrue();
	}

	/**
	 */
	@Test
	void listenerDontRunIfTransactionFails() {
		final SimpleCommitListener listener = new SimpleCommitListener();

		try {
			ofy().transactNew(1, () -> {
				final AsyncTransaction txn = ofy().getTransaction();
				txn.listenForCommit(listener);

				throw new ConcurrentModificationException();
			});
		} catch (ConcurrentModificationException e) {}

		assertThat(listener.hasRun()).isFalse();
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
	void listenerIsOnlyCalledOnceIfTransactionRetries() {
		final CommitCountListener listener = new CommitCountListener();
		final Counter counter = new Counter();

		ofy().transactNew(3, () -> {
			counter.counter++;
			final AsyncTransaction txn = ofy().getTransaction();
			txn.listenForCommit(listener);

			if (counter.counter < 3) {
				throw new DatastoreException(10, "too much contention on these datastore entities", "ABORTED");
			}
		});

		assertThat(counter.counter).isEqualTo(3);
		assertThat(listener.getCommitCount()).isEqualTo(1);
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

				final Trivial triv1 = ofy().transactionless(() -> ofy().load().key(tk).now());
				final Trivial triv2 = ofy().load().key(tk).now();

				ofy().transactionless(() -> ofy().save().entity(triv1).now());
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

			final Trivial triv1 = ofy().transactionless(() -> ofy().load().key(tk).now());
			final Trivial triv2 = ofy().load().key(tk).now();

			if (counter.counter < 3) {
				ofy().transactionless(() -> ofy().save().entity(triv1).now());
			}
			ofy().save().entity(triv2).now();
		});

		assertThat(counter.counter).isEqualTo(3);
		assertThat(listener.getCommitCount()).isEqualTo(1);
	}

	/**
	 */
	@Test
	void executeWithRequiresNewCreatesNewTransaction() {
		ofy().transact(() -> {
			final AsyncTransaction txn = ofy().getTransaction();

			ofy().execute(TxnType.REQUIRES_NEW, () -> {
				assertThat(ofy().getTransaction()).isNotEqualTo(txn);
			});
		});
	}
}