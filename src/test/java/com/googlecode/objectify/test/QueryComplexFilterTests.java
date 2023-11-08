/*
 */

package com.googlecode.objectify.test;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.test.entity.Trivial;
import com.googlecode.objectify.test.util.TestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.google.common.truth.Truth.assertThat;
import static com.googlecode.objectify.ObjectifyService.factory;
import static com.googlecode.objectify.ObjectifyService.ofy;
import static com.googlecode.objectify.cmd.Filter.and;
import static com.googlecode.objectify.cmd.Filter.equalTo;
import static com.googlecode.objectify.cmd.Filter.greaterThan;
import static com.googlecode.objectify.cmd.Filter.greaterThanOrEqualTo;
import static com.googlecode.objectify.cmd.Filter.lessThan;
import static com.googlecode.objectify.cmd.Filter.lessThanOrEqualTo;
import static com.googlecode.objectify.cmd.Filter.or;

/**
 * Exercise the Filter class.
 */
class QueryComplexFilterTests extends TestBase {

	/** */
	private Trivial triv1;
	private Trivial triv2;
	private Trivial triv3;
	private List<Key<Trivial>> keys;

	/** */
	@BeforeEach
	void setUpExtra() {
		factory().register(Trivial.class);

		this.triv1 = new Trivial("foo1", 1);
		this.triv2 = new Trivial("foo2", 2);
		this.triv3 = new Trivial("foo3", 3);

		final Map<Key<Trivial>, Trivial> result = ofy().save().entities(triv1, triv2, triv3).now();

		this.keys = new ArrayList<>(result.keySet());
	}

	/** */
	@Test
	void simpleConditions() throws Exception {
		{
			final List<Trivial> list = ofy().load().type(Trivial.class).filter(equalTo("someString", "foo2")).list();
			assertThat(list).containsExactly(triv2);
		}

		{
			final List<Trivial> list = ofy().load().type(Trivial.class).filter(greaterThan("someString", "foo2")).list();
			assertThat(list).containsExactly(triv3);
		}

		{
			final List<Trivial> list = ofy().load().type(Trivial.class).filter(greaterThanOrEqualTo("someString", "foo2")).list();
			assertThat(list).containsExactly(triv2, triv3);
		}

		{
			final List<Trivial> list = ofy().load().type(Trivial.class).filter(lessThan("someString", "foo2")).list();
			assertThat(list).containsExactly(triv1);
		}

		{
			final List<Trivial> list = ofy().load().type(Trivial.class).filter(lessThanOrEqualTo("someString", "foo2")).list();
			assertThat(list).containsExactly(triv1, triv2);
		}
	}

	/** */
	@Test
	void compositeAndConditions() throws Exception {
		final List<Trivial> list = ofy().load().type(Trivial.class).filter(
					and(
							greaterThan("someString", "foo1"),
							lessThan("someString", "foo3")
					)
				).list();
		assertThat(list).containsExactly(triv2);
	}

	/**
	 * The datastore emulator gives us "unsupported composite property operator", so the test is disabled.
	 * But this is part of the google library's API; it should work in production.
	 * Last checked with google-cloud-datastore:2.17.5
	 */
	@Test
	@Disabled
	void compositeOrConditions() throws Exception {
		final List<Trivial> list = ofy().load().type(Trivial.class).filter(
				or(
						greaterThan("someString", "foo2"),
						lessThan("someString", "foo2")
				)
		).list();
		assertThat(list).containsExactly(triv1, triv3);
	}
}
