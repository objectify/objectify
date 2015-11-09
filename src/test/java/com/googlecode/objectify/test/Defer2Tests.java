/*
 */

package com.googlecode.objectify.test;

import com.googlecode.objectify.VoidWork;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.OnSave;
import com.googlecode.objectify.test.entity.Trivial;
import com.googlecode.objectify.test.util.TestBase;
import lombok.Data;
import org.testng.annotations.Test;

import static com.googlecode.objectify.test.util.TestObjectifyService.fact;
import static com.googlecode.objectify.test.util.TestObjectifyService.ofy;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Tests of defer()
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class Defer2Tests extends TestBase
{
	@Entity
	@Data
	static class HasOnSaveThatDefers {
		@Id private Long id;

		@OnSave void deferMoreStuff() {
			ofy().defer().save().entity(new Trivial("foo", 123));
		}
	}

	/**
	 * Let's say you defer a save of an entity that has an @OnSave method that itself defers
	 * save of more entities. That should work.
	 */
	@Test
	public void deferredSaveWithinOnSaveMethodSaves() throws Exception {
		fact().register(Trivial.class);
		fact().register(HasOnSaveThatDefers.class);

		ofy().transact(new VoidWork() {
			@Override
			public void vrun() {
				HasOnSaveThatDefers h = new HasOnSaveThatDefers();
				ofy().defer().save().entity(h);
			}
		});

		final Trivial triv = ofy().load().type(Trivial.class).first().now();
		assertThat(triv, notNullValue());
	}
}