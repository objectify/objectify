/*
 */

package com.googlecode.objectify.test;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.test.entity.Trivial;
import com.googlecode.objectify.test.util.LocalMemcacheExtension;
import com.googlecode.objectify.test.util.MockitoExtension;
import com.googlecode.objectify.test.util.RemoteObjectifyExtension;
import com.googlecode.objectify.test.util.TestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.google.common.truth.Truth.assertThat;
import static com.googlecode.objectify.ObjectifyService.factory;
import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * The datastore emulator does not yet support certain operations. These run against a production database.
 */
@ExtendWith({
		MockitoExtension.class,
		LocalMemcacheExtension.class,
		RemoteObjectifyExtension.class,
})
class RemoteQueryTests extends TestBase {

	/** */
	private Trivial triv1;
	private Trivial triv2;
	private List<Key<Trivial>> keys;

	/** */
	@BeforeEach
	void setUp() {
		factory().register(Trivial.class);

		this.triv1 = new Trivial("foo1", 1);
		this.triv2 = new Trivial("foo2", 2);

		final Map<Key<Trivial>, Trivial> result = ofy().save().entities(triv1, triv2).now();

		this.keys = new ArrayList<>(result.keySet());
	}

	@Test
	void testNotEquals() throws Exception {
		{
			final List<Trivial> result = ofy().load().type(Trivial.class).filter("someString !=", "foo1").list();
			assertThat(result).containsExactly(triv2);
		}

		{
			final List<Trivial> result = ofy().load().type(Trivial.class).filter("someString <>", "foo2").list();
			assertThat(result).containsExactly(triv1);
		}
	}
}
