/*
 */

package com.googlecode.objectify.test;

import com.google.cloud.datastore.DatastoreException;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.TxnOptions;
import com.googlecode.objectify.test.entity.Trivial;
import com.googlecode.objectify.test.util.TestBase;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static com.google.common.truth.Truth.assertThat;
import static com.googlecode.objectify.ObjectifyService.factory;
import static com.googlecode.objectify.ObjectifyService.ofy;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests of readonly transactional behavior.
 */
class TransactionReadOnlyTests extends TestBase {

	@Test
	void exerciseSimpleReadOnlyTransactions() throws Exception {
		factory().register(Trivial.class);

		final Trivial triv = new Trivial("foo", 5);

		final Key<Trivial> k = ofy().transact(() -> ofy().save().entity(triv).now());

		ofy().transactReadOnly(() -> {
			final Trivial fetched = ofy().load().key(k).now();

			assertThat(fetched).isEqualTo(triv);
		});
	}

	@Test
	void runASnapshotQuery() throws Exception {
		factory().register(Trivial.class);

		final Trivial triv = new Trivial("foo", 5);

		final Key<Trivial> k = ofy().transact(() -> ofy().save().entity(triv).now());

		final TxnOptions opts = TxnOptions.deflt().readOnly(true).readTime(Instant.now());
		ofy().transact(opts, () -> {
			final Trivial fetched = ofy().load().key(k).now();

			assertThat(fetched).isEqualTo(triv);
		});
	}

	@Test
	void writingInAReadOnlyTransactionFails() throws Exception {
		factory().register(Trivial.class);

		final Trivial triv = new Trivial("foo", 5);

		assertThrows(DatastoreException.class, () -> {
			ofy().transactReadOnly(() -> ofy().save().entity(triv).now());
		});
	}

	@Test
	void transactionCanBeInherited() throws Exception {
		factory().register(Trivial.class);

		final Trivial triv = new Trivial("foo", 5);

		ofy().transact(() -> {
			final Key<Trivial> k = ofy().save().entity(triv).now();

			ofy().transactReadOnly(() -> {
				final Trivial fetched = ofy().load().key(k).now();
				assertThat(fetched).isEqualTo(triv);
			});
		});
	}


	@Test
	void transactionThatStartsReadWriteStaysReadWrite() throws Exception {
		factory().register(Trivial.class);

		final Trivial triv = new Trivial("foo", 5);

		ofy().transact(() -> {
			final Key<Trivial> k = ofy().save().entity(triv).now();

			ofy().transactReadOnly(() -> {
				final Trivial triv2 = new Trivial("bar", 5);
				final Key<Trivial> k2 = ofy().save().entity(triv2).now();
				assertThat(k2).isNotEqualTo(k);
			});
		});
	}

	@Test
	void transactionThatStartsReadOnlyStaysReadOnly() throws Exception {
		factory().register(Trivial.class);

		assertThrows(DatastoreException.class, () -> {
			ofy().transactReadOnly(() -> {
				ofy().transact(() -> {
					final Trivial triv = new Trivial("foo", 5);

					ofy().save().entity(triv).now();
				});
			});
		});
	}
}