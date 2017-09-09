/*
 */

package com.googlecode.objectify.test;

import com.google.appengine.api.datastore.Transaction;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyOptions;
import com.googlecode.objectify.TxnType;
import com.googlecode.objectify.VoidWork;
import com.googlecode.objectify.Work;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.cmd.Deferred;
import com.googlecode.objectify.cmd.Deleter;
import com.googlecode.objectify.cmd.Loader;
import com.googlecode.objectify.cmd.Saver;
import com.googlecode.objectify.impl.ObjectifyImpl;
import com.googlecode.objectify.impl.TransactionImpl;
import com.googlecode.objectify.test.entity.Trivial;
import com.googlecode.objectify.test.util.TestBase;
import com.googlecode.objectify.test.util.TestObjectifyService;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.logging.Logger;

import static com.googlecode.objectify.test.util.TestObjectifyService.fact;
import static com.googlecode.objectify.test.util.TestObjectifyService.ofy;

/**
 * Tests of transactional behavior.  Since many transactional characteristics are
 * determined by race conditions and other realtime effects, these tests are not
 * very thorough.  We will assume that Google's transactions work.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class TransactionTests extends TestBase
{
	/** */
	@SuppressWarnings("unused")
	private static Logger log = Logger.getLogger(TransactionTests.class.getName());

	/** */
	@Test
	public void testSimpleTransaction() throws Exception {
		fact().register(Trivial.class);

		final Trivial triv = new Trivial("foo", 5);

		Key<Trivial> k = ofy().transact(new Work<Key<Trivial>>() {
			@Override
			public Key<Trivial> run() {
				return ofy().save().entity(triv).now();
			}
		});

		Trivial fetched = ofy().load().key(k).now();

		assert fetched.getId().equals(k.getId());
		assert fetched.getSomeNumber() == triv.getSomeNumber();
		assert fetched.getSomeString().equals(triv.getSomeString());
	}

	/** */
	@Entity
	@Cache
	static class HasSimpleCollection {
		@Id Long id;
		List<String> stuff = new ArrayList<>();
	}

	/** */
	@Test
	public void testInAndOutOfTransaction() throws Exception {
		fact().register(HasSimpleCollection.class);

		final HasSimpleCollection simple = new HasSimpleCollection();
		ofy().save().entity(simple).now();

		HasSimpleCollection simple2 = ofy().transact(new Work<HasSimpleCollection>() {
			@Override
			public HasSimpleCollection run() {
				HasSimpleCollection simple2 = ofy().load().type(HasSimpleCollection.class).id(simple.id).now();
				simple2.stuff.add("blah");
				ofy().save().entity(simple2);
				return simple2;
			}
		});

		ofy().clear();
		HasSimpleCollection simple3 = ofy().load().type(HasSimpleCollection.class).id(simple.id).now();

		assert simple2.stuff.equals(simple3.stuff);
	}

	/**
	 * This should theoretically test the case where the cache is being modified even after a concurrency failure.
	 * However, it doesn't seem to trigger even without the logic fix in ListenableFuture.
	 */
	@Test
	public void testConcurrencyFailure() throws Exception {
		fact().register(Trivial.class);

		Trivial triv = new Trivial("foo", 5);
		final Key<Trivial> tk = ofy().save().entity(triv).now();

		try {
			ofy().transactNew(2, new VoidWork() {
				@Override
				public void vrun() {
					Trivial triv1 = ofy().transactionless().load().key(tk).now();
					Trivial triv2 = ofy().load().key(tk).now();

					triv1.setSomeString("bar");
					triv2.setSomeString("shouldn't work");

					ofy().transactionless().save().entity(triv1).now();
					ofy().save().entity(triv2).now();
				}
			});
			assert false;	// must throw exception
		}
		catch (ConcurrentModificationException ex) {}

		Trivial fetched = ofy().load().key(tk).now();

		// This will be fetched from the cache, and must not be the "shouldn't work"
		assert fetched.getSomeString().equals("bar");
	}

	/**
	 */
	@Test
	public void testTransactWork() throws Exception {
		fact().register(Trivial.class);

		final Trivial triv = new Trivial("foo", 5);
		ofy().save().entity(triv).now();

		Trivial updated = ofy().transact(new Work<Trivial>() {
			@Override
			public Trivial run() {
				Trivial result = ofy().load().entity(triv).now();
				result.setSomeNumber(6);
				ofy().save().entity(result);
				return result;
			}
		});
		assert updated.getSomeNumber() == 6;

		Trivial fetched = ofy().load().entity(triv).now();
		assert fetched.getSomeNumber() == 6;
	}

	/**
	 * Make sure that an async delete in a transaction fixes the session cache when the transaction is committed.
	 */
	@Test
	public void testAsyncDelete() throws Exception {
		fact().register(Trivial.class);

		final Trivial triv = new Trivial("foo", 5);

		// Make sure it's in the session (and memcache for that matter)
		ofy().saveClearLoad(triv);

		ofy().transact(new Work<Void>() {
			@Override
			public Void run() {
				// Load this, enlist in txn
				Trivial fetched = ofy().load().entity(triv).now();

				// Do this async, don't complete it manually
				ofy().delete().entity(fetched);

				return null;
			}
		});

		assert ofy().load().entity(triv).now() == null;
	}

	/** For transactionless tests */
	@Entity
	@Cache
	public static class Thing {
		@Id long id;
		String foo;
		public Thing() {}
		public Thing(long id) { this.id = id; this.foo = "foo"; }
	}

	/** */
	@Test
	public void testTransactionless() throws Exception {
		fact().register(Thing.class);

		for (int i=1; i<10; i++) {
			Thing th = new Thing(i);
			ofy().save().entity(th).now();
		}

		ofy().transact(new Work<Void>() {
			@Override
			public Void run() {
				for (int i=1; i<10; i++)
					ofy().transactionless().load().type(Thing.class).id(i).now();

				ofy().save().entity(new Thing(99));
				return null;
			}
		});
	}

	/**
	 */
	@Test
	public void testTransactionRollback() throws Exception {
		fact().register(Trivial.class);

		try {
			ofy().transact(new VoidWork() {
				@Override
				public void vrun() {
					Trivial triv = new Trivial("foo", 5);
					ofy().save().entity(triv).now();
					throw new RuntimeException();
				}
			});
		} catch (RuntimeException ex) {}

		// Now verify that it was not saved
		Trivial fetched = ofy().load().type(Trivial.class).first().now();
		assert fetched == null;
	}

	/**
	 * This is a somewhat clunky way to test this, and requires making impl.getCache() public,
	 * but it gets the job done.
	 */
	@Test
	public void transactionalObjectifyInheritsCacheSetting() throws Exception {
		try (Objectify ofy = fact().begin(new ObjectifyOptions().cache(false))) {
			ofy.transact(new VoidWork() {
				@Override
				public void vrun() {
					// Test in _and out_ of a transaction
					{
						ObjectifyImpl<?> txnlessImpl = (ObjectifyImpl<?>) ofy;
						assert !txnlessImpl.getCache();
					}
					{
						ObjectifyImpl<?> txnlessImpl = (ObjectifyImpl<?>) ofy.transactionless();
						assert !txnlessImpl.getCache();
					}
				}
			});
		}
	}

	/**
	 */
	@Test
	public void executeMethodWorks() throws Exception {
		ofy().execute(TxnType.REQUIRED, new VoidWork() {
			@Override
			public void vrun() {
				assert ofy().load().type(Trivial.class).id(123L).now() == null;
			}
		});
	}

	public static class Counter {
		public int counter = 0;
	}

	/**
	 */
	@Test
	public void limitsTries() throws Exception {
		final Counter counter = new Counter();

		try {
			ofy().transactNew(3, new VoidWork() {
				@Override
				public void vrun() {
					counter.counter++;
					throw new ConcurrentModificationException();
				}
			});
		} catch (ConcurrentModificationException e) {}

		assert counter.counter == 3;
	}

	public static class SimpleCommitListener implements Runnable {
		private boolean run = false;

		@Override
		public void run() {
			run = true;
		}

		public boolean hasRun() {
			return run;
		}
	}

	/**
	 */
	@Test
	public void transactionListeners() {
		final SimpleCommitListener listener = new SimpleCommitListener();

		ofy().transact(new VoidWork() {
			@Override
			public void vrun() {
				TransactionImpl txn = ofy().getTransaction();
				txn.listenForCommit(listener);

				assert !listener.hasRun();
			}
		});

		assert listener.hasRun();
	}

	/**
	 */
	@Test
	public void listenerDontRunIfTransactionFails() {
		final SimpleCommitListener listener = new SimpleCommitListener();

		try {
			ofy().transactNew(1, new VoidWork() {
				@Override
				public void vrun() {
					TransactionImpl txn = ofy().getTransaction();
					txn.listenForCommit(listener);

					throw new ConcurrentModificationException();
				}
			});
		} catch (ConcurrentModificationException e) {}


		assert !listener.hasRun();
	}

	public static class CommitCountListener implements Runnable {
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
	public void listenerIsOnlyCalledOnceIfTransactionRetries() {
		final CommitCountListener listener = new CommitCountListener();
		final Counter counter = new Counter();

		ofy().transactNew(3, new VoidWork() {
			@Override
			public void vrun() {
				counter.counter++;
				TransactionImpl txn = ofy().getTransaction();
				txn.listenForCommit(listener);

				if (counter.counter < 3) {
					throw new ConcurrentModificationException();
				}
			}
		});

		assert counter.counter == 3;
		assert listener.getCommitCount() == 1;
	}

	/**
	 */
	@Test
	public void testListenerNotCalledWithOrganicConcurrencyFailure() throws Exception {
		fact().register(Trivial.class);

		Trivial triv = new Trivial("foo", 5);
		final Key<Trivial> tk = ofy().save().entity(triv).now();
		final SimpleCommitListener listener = new SimpleCommitListener();

		try {
			ofy().transactNew(1, new VoidWork() {
				@Override
				public void vrun() {
					TransactionImpl txn = ofy().getTransaction();
					txn.listenForCommit(listener);

					Trivial triv1 = ofy().transactionless().load().key(tk).now();
					Trivial triv2 = ofy().load().key(tk).now();


					ofy().transactionless().save().entity(triv1).now();
					ofy().save().entity(triv2).now();
				}
			});
			assert false;	// must throw exception
		}
		catch (ConcurrentModificationException ex) {}

		assert !listener.hasRun();
	}

	/**
	 */
	@Test
	public void listenerIsOnlyCalledOnceIfTransactionRetriesFromOrganicConcurrencyFailure() {
		fact().register(Trivial.class);

		Trivial triv = new Trivial("foo", 5);
		final Key<Trivial> tk = ofy().save().entity(triv).now();
		final CommitCountListener listener = new CommitCountListener();
		final Counter counter = new Counter();

		ofy().transactNew(3, new VoidWork() {
			@Override
			public void vrun() {
				counter.counter++;
				TransactionImpl txn = ofy().getTransaction();
				txn.listenForCommit(listener);

				Trivial triv1 = ofy().transactionless().load().key(tk).now();
				Trivial triv2 = ofy().load().key(tk).now();


				if (counter.counter < 3) {
					ofy().transactionless().save().entity(triv1).now();
				}
				ofy().save().entity(triv2).now();
			}
		});

		assert counter.counter == 3;
		assert listener.getCommitCount() == 1;
	}

	/**
	 */
	@Test
	public void executeWithRequiresNewCreatesNewTransaction() {
		ofy().transact(new VoidWork() {
			@Override
			public void vrun() {

				final Transaction txn = ofy().getTransaction();

				ofy().execute(TxnType.REQUIRES_NEW, new VoidWork() {
					@Override
					public void vrun() {
						assert txn != ofy().getTransaction();
					}
				});
			}
		});
	}

	@Test
	public void loaderIsBoundToTransactorAtInitialization() {
		fact().register(Trivial.class);
		final Trivial triv = new Trivial(42l, "foo", 5);

		ofy().transact(new VoidWork() {
			@Override
			public void vrun() {
				ofy().save().entity(triv).now();

				Loader loader = ofy().transactionless().load();
				Trivial fetched = loader.entity(triv).now();
				assert fetched == null;
			}
		});

		// Now verify that it was saved
		Trivial fetched = ofy().load().entity(triv).now();
		assert fetched != null;
	}

	@Test
	public void loaderIsBoundToTransactorAtInitialization2() {
		fact().register(Trivial.class);
		final Trivial triv = new Trivial(42l, "foo", 5);
		ofy().save().entity(triv).now();
		ofy().clear(); // important to make sure that the read below hits the datastore

		try {
			ofy().transactNew(1, new VoidWork() {
				@Override
				public void vrun() {
					// The loader is initialized in a transactionless. We expect that the loader is bound to the
					// transactionless context.
					Loader loader = ofy().transactionless().load();
					loader.entity(triv).now();

					// We perform a write to a different entity group. If the loader touched the active transaction,
					// and the entity was modified outside of the transaction, a ConcurrentModificationException will
					// be thrown. If the loader didn't touch the transaction, it would commit successfully.
					ofy().save().entity(new Trivial(43l, "bar", 6)).now();

					ofy().transactionless().save().entity(triv);
				}
			});
		} catch (ConcurrentModificationException e) {
			assert false : "transactionless loader was not fully isolated from the encapsulating transaction";
		}
	}

	@Test
	public void saverIsBoundToTransactorAtInitialization() {
		fact().register(Trivial.class);
		final Trivial triv = new Trivial(42l, "foo", 5);

		ofy().transact(new VoidWork() {
			@Override
			public void vrun() {
				final Key<Trivial> trivKey = Key.create(Trivial.class, 42l);
				assert !ofy().isLoaded(trivKey);

				Saver saver = ofy().transactionless().save();
				triv.setSomeString("bar");
				saver.entity(triv).now();

				// A write is visible in the session (test with isLoaded()) it was performed. We expect it to not be
				// visible in the transaction but to be visible outside.
				assert !ofy().isLoaded(trivKey);

				ofy().transactionless().isLoaded(trivKey);
			}
		});
	}

	@Test
	public void deleterIsBoundToTransactorAtInitialization() {
		fact().register(Trivial.class);
		final Trivial triv = new Trivial(42l, "foo", 5);
		ofy().save().entity(triv).now();

		ofy().transact(new VoidWork() {
			@Override
			public void vrun() {
				Trivial fetched = ofy().load().entity(triv).now();

				Deleter deleter = ofy().transactionless().delete();
				deleter.entity(triv).now();

				assert null != ofy().load().entity(triv).now();

				ofy().transactionless().load().entity(triv).now();
			}
		});
	}

	@Test
	public void deferredIsBoundToTransactorAtInitialization() {
		fact().register(Trivial.class);
		final Trivial triv1 = new Trivial(42l, "foo", 5);
		final Trivial triv2 = new Trivial(43l, "foo", 5);

		ofy().save().entity(triv1).now();

		assert null != ofy().load().entity(triv1).now();
		assert null == ofy().load().entity(triv2).now();

		final Objectify transactionless = ofy().transact(new Work<Objectify>() {
			@Override
			public Objectify run() {
				Objectify transactionless = ofy().transactionless();
				Deferred deferred = transactionless.defer();
				deferred.delete().entity(triv1);
				deferred.save().entity(triv2);

				// deferred operations not visible inside the transaction
				assert null != ofy().load().entity(triv1).now();
				assert null == ofy().load().entity(triv2).now();

				return transactionless;
			}
		});

		ofy().clear(); // otherwise writes will be visible from merged cache
		assert null != ofy().load().entity(triv1).now();
		assert null == ofy().load().entity(triv2).now();

		// NOTE: unless we saved off the ofy returned from transactionless(), we have no way of flushing
		transactionless.flush(); // writes are visible once deferred are flushed
		assert null == ofy().load().entity(triv1).now();
		assert null != ofy().load().entity(triv2).now();
	}
}