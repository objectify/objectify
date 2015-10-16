/*
 */

package com.googlecode.objectify.test;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.TxnType;
import com.googlecode.objectify.VoidWork;
import com.googlecode.objectify.Work;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.impl.ObjectifyImpl;
import com.googlecode.objectify.impl.TransactionImpl;
import com.googlecode.objectify.test.entity.Trivial;
import com.googlecode.objectify.test.util.TestBase;
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
		ofy().cache(false).transact(new VoidWork() {
			@Override
			public void vrun() {
				// Test in _and out_ of a transaction
				ObjectifyImpl<?> txnlessImpl = (ObjectifyImpl<?>)ofy().transactionless();
				assert !txnlessImpl.getCache();
			}
		});
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
}