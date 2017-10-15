/*
 */

package com.googlecode.objectify.test;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.OnSave;
import com.googlecode.objectify.test.entity.Trivial;
import com.googlecode.objectify.test.util.TestBase;
import lombok.Data;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static com.googlecode.objectify.ObjectifyService.factory;
import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * Tests of defer()
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
class DeferTests2 extends TestBase {
	@Entity
	@Data
	private static class HasOnSaveThatDefers {
		@Id
		private Long id;

		@OnSave
		void deferMoreStuff() {
			ofy().defer().save().entity(new Trivial("foo", 123));
		}
	}

	/**
	 * Let's say you defer a save of an entity that has an @OnSave method that itself defers
	 * save of more entities. That should work.
	 */
	@Test
	void deferredSaveWithinOnSaveMethodSaves() throws Exception {
		factory().register(Trivial.class);
		factory().register(HasOnSaveThatDefers.class);

		ofy().transact(() -> {
			final HasOnSaveThatDefers h = new HasOnSaveThatDefers();
			ofy().defer().save().entity(h);
		});

		final Trivial triv = ofy().load().type(Trivial.class).first().now();
		assertThat(triv).isNotNull();
	}
}