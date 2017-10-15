/*
 */

package com.googlecode.objectify.test;

import com.google.appengine.api.datastore.Entity;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.AlsoLoad;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Load;
import com.googlecode.objectify.test.entity.Trivial;
import com.googlecode.objectify.test.util.TestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static com.googlecode.objectify.ObjectifyService.factory;
import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * More tests of using the @AlsoLoad annotation combined with @Load
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
class LoadAlsoLoadTests extends TestBase {
	/** */
	@com.googlecode.objectify.annotation.Entity
	@Cache
	private static class HasConcrete {
		@Id Long id;
		String bar;

		void cruft(@Load @AlsoLoad("foo") Ref<Trivial> triv) {
			this.bar = triv.get().getSomeString();
		}
	}

	/**
	 */
	@BeforeEach
	void setUpExtra() {
		factory().register(HasConcrete.class);
		factory().register(Trivial.class);
	}

	/** */
	@Test
	void alsoLoadWorksWithLoad() throws Exception {
		final Trivial triv = new Trivial("someString", 123L);
		final Key<Trivial> trivKey = ofy().save().entity(triv).now();

		final Entity ent = new Entity(Key.getKind(HasConcrete.class));
		ent.setProperty("foo", trivKey.getRaw());
		ds().put(null, ent);

		final Key<HasConcrete> key = Key.create(ent.getKey());
		final HasConcrete fetched = ofy().load().key(key).now();

		assertThat(fetched.bar).isEqualTo(triv.getSomeString());
	}
}