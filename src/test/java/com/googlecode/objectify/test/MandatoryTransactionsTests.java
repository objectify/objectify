/*
 */

package com.googlecode.objectify.test;

import com.googlecode.objectify.test.entity.Trivial;
import com.googlecode.objectify.test.util.TestBase;
import org.junit.jupiter.api.Test;

import static com.googlecode.objectify.ObjectifyService.factory;
import static com.googlecode.objectify.ObjectifyService.ofy;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests to ensure the ofy().mandatoryTransactions() flag works
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
class MandatoryTransactionsTests extends TestBase {
	/** */
	@Test
	void requireMandatoryTransactionsForSave() throws Exception {
		factory().register(Trivial.class);

		final Trivial triv = new Trivial();

		assertThrows(IllegalStateException.class, () -> ofy().mandatoryTransactions(true).save().entity(triv).now());
	}

	/** */
	@Test
	void requireMandatoryTransactionsForDelete() throws Exception {
		factory().register(Trivial.class);

		assertThrows(IllegalStateException.class, () -> ofy().mandatoryTransactions(true).delete().type(Trivial.class).id(123L).now());

	}

	/** */
	@Test
	void mandatoryTransactionsWorkingForSave() throws Exception {
		factory().register(Trivial.class);

		ofy().transact(() -> {
			final Trivial triv = new Trivial();
			ofy().mandatoryTransactions(true).save().entity(triv).now();
		});
	}

	/** */
	@Test
	void mandatoryTransactionsWorkingForDelete() throws Exception {
		factory().register(Trivial.class);

		ofy().transact(() -> {
			ofy().mandatoryTransactions(true).delete().type(Trivial.class).id(123L).now();
		});
	}
}