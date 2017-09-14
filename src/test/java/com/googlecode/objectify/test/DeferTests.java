/*
 */

package com.googlecode.objectify.test;

import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.VoidWork;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.OnSave;
import com.googlecode.objectify.test.entity.Trivial;
import com.googlecode.objectify.test.util.GAETestBase;
import com.googlecode.objectify.test.util.TestObjectifyFactory;
import com.googlecode.objectify.test.util.TestObjectifyService;
import com.googlecode.objectify.util.Closeable;
import lombok.Data;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static com.googlecode.objectify.test.util.TestObjectifyService.ds;
import static com.googlecode.objectify.test.util.TestObjectifyService.fact;
import static com.googlecode.objectify.test.util.TestObjectifyService.ofy;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

/**
 * Tests of defer()
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class DeferTests extends GAETestBase
{
	private void assertIsInSession(Trivial triv) {
		assertThat(ofy().isLoaded(Key.create(triv)), is(true));
		assertThat(ofy().load().entity(triv).now(), is(triv));
	}

	private void assertIsNotInSession(Trivial triv) {
		assertThat(ofy().load().entity(triv).now(), nullValue());
	}

	private void assertIsInDatastore(Trivial triv) {
		try {
			ds().get(null, Key.create(triv).getRaw());
		} catch (EntityNotFoundException e) {
			assert false : "Entity should be in the datastore.";
		}
	}

	private void assertIsNotInDatastore(Trivial triv) {
		try {
			ds().get(null, Key.create(triv).getRaw());
			assert false : "Entity should not be in the datastore.";
		} catch (EntityNotFoundException e) {
			// success
		}
	}

	private MemcacheService mc() {
		return MemcacheServiceFactory.getMemcacheService(ObjectifyFactory.MEMCACHE_NAMESPACE);
	}

	private void assertIsInMemcache(Trivial triv) {
		String stringKey = Key.create(triv).getString();
		Object actual = mc().get(stringKey);
		assertThat(actual, not(nullValue()));
	}

	private void assertIsNotInMemcache(Trivial triv) {
		String stringKey = Key.create(triv).getString();
		Trivial actual = (Trivial) mc().get(stringKey);
		assertThat(actual, nullValue());
	}

	@BeforeMethod
	public void setUp() throws Exception {
		TestObjectifyService.setFactory(new TestObjectifyFactory());

		fact().register(Trivial.class);
	}

	/** */
	@Test
	public void deferredSaveWithAutogeneratedId() throws Exception {

		Trivial triv = new Trivial("foo", 5);

		try (Closeable root = TestObjectifyService.begin()) {
			ofy().defer().save().entity(triv);

			assert triv.getId() == null;
		}

		assert triv.getId() != null;
	}

	/** */
	@Test
	public void deferredSaveAndDeleteProcessedAtEndOfRequest() throws Exception {

		Trivial triv = new Trivial(123L, "foo", 5);

		try (Closeable root = TestObjectifyService.begin()) {
			ofy().defer().save().entity(triv);

			assertIsInSession(triv);
			assertIsNotInDatastore(triv);
		}

		try (Closeable root = TestObjectifyService.begin()) {
			Trivial loaded = ofy().load().entity(triv).now();
			assertThat(loaded, equalTo(triv));
		}

		try (Closeable root = TestObjectifyService.begin()) {
			ofy().defer().delete().entity(triv);

			assertIsNotInSession(triv);
			assertIsInDatastore(triv);
		}

		try (Closeable root = TestObjectifyService.begin()) {
			Trivial loaded = ofy().load().entity(triv).now();
			assertThat(loaded, nullValue());
		}
	}

	/** */
	@Test
	public void deferredSaveAndDeleteProcessedAtEndOfTransaction() throws Exception {

		final Trivial triv = new Trivial(123L, "foo", 5);

		try (Closeable root = TestObjectifyService.begin()) {

			ofy().transact(new VoidWork() {
				@Override
				public void vrun() {
					ofy().defer().save().entity(triv);

					assertIsInSession(triv);
					assertIsNotInDatastore(triv);
				}
			});

			{
				Trivial loaded = ofy().load().entity(triv).now();
				assertThat(loaded, equalTo(triv));
			}

			ofy().transact(new VoidWork() {
				@Override
				public void vrun() {
					ofy().defer().delete().entity(triv);

					assertIsNotInSession(triv);
					assertIsInDatastore(triv);
				}
			});

			{
				Trivial loaded = ofy().load().entity(triv).now();
				assertThat(loaded, nullValue());
			}
		}
	}

	@Entity
	@Data
	static class HasOnSave {
		@Id private Long id;
		private String data;

		@OnSave void changeData() {
			data = "onsaved";
		}
	}

	/** */
	@Test
	public void deferredSaveTriggersOnSaveMethods() throws Exception {
		fact().register(HasOnSave.class);
		final HasOnSave hos = new HasOnSave();

		try (Closeable root = TestObjectifyService.begin()) {
			ofy().defer().save().entity(hos);
		}

		assertThat(hos.getData(), equalTo("onsaved"));
	}

	@Test
	public void testTransactionTransactionlessDeferredNotFlushed() {

		final Trivial triv = new Trivial(123L, "foo", 5);

		try (Closeable root = TestObjectifyService.begin()) {
			ofy().transact(new VoidWork() {
				@Override
				public void vrun() {
					ofy().transactionless().defer().save().entity(triv);
				}
			});

			assertIsInSession(triv);
			assertIsNotInDatastore(triv);
		}

		// ofy().transaction().transactionless() creates a fresh ObjectifyImpl that is not tracked anywhere
		// so nothing flushes the deferred operations.
		// assertIsInDatastore(triv);
		assertIsNotInDatastore(triv);

		// ofy() already maintains a stack each time it switches from non-transactional to transaction context.
		// Instead of transactionless() returning a new Objectify instance outside of that stack, it can return
		// the first transactionless instance up the stack. If we don't have any lose Objectify instances, we
		// can guarantee that we can flush and pending operations for a proper cleanup.
	}
}
