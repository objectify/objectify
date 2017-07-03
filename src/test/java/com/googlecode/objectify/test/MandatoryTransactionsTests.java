/*
 */

package com.googlecode.objectify.test;

import com.googlecode.objectify.test.entity.Trivial;
import com.googlecode.objectify.test.util.TestBase;
import org.testng.annotations.Test;

import static com.googlecode.objectify.test.util.TestObjectifyService.fact;
import static com.googlecode.objectify.test.util.TestObjectifyService.ofy;

/**
 * Tests to ensure the ofy().mandatoryTransactions() flag works
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class MandatoryTransactionsTests extends TestBase
{
	/** */
	@Test(expectedExceptions = IllegalStateException.class)
	public void requireMandatoryTransactionsForSave() throws Exception {
		fact().register(Trivial.class);

		Trivial triv = new Trivial();
		ofy().mandatoryTransactions(true).save().entity(triv).now();
	}

	/** */
	@Test(expectedExceptions = IllegalStateException.class)
	public void requireMandatoryTransactionsForDelete() throws Exception {
		fact().register(Trivial.class);

		ofy().mandatoryTransactions(true).delete().type(Trivial.class).id(123L).now();
	}

	/** */
	@Test
	public void mandatoryTransactionsWorkingForSave() throws Exception {
		fact().register(Trivial.class);

		ofy().transact(new Runnable() {
			@Override
			public void run() {
				final Trivial triv = new Trivial();
				ofy().mandatoryTransactions(true).save().entity(triv).now();
			}
		});
	}

	/** */
	@Test
	public void mandatoryTransactionsWorkingForDelete() throws Exception {
		fact().register(Trivial.class);

		ofy().transact(new Runnable() {
			@Override
			public void run() {
				ofy().mandatoryTransactions(true).delete().type(Trivial.class).id(123L).now();
			}
		});
	}
}