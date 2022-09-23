/*
 */

package com.googlecode.objectify.test;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.test.entity.Employee;
import com.googlecode.objectify.test.entity.Trivial;
import com.googlecode.objectify.test.util.LocalMemcacheExtension;
import com.googlecode.objectify.test.util.MockitoExtension;
import com.googlecode.objectify.test.util.RemoteObjectifyExtension;
import com.googlecode.objectify.test.util.TestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

	@Test
	void testIN() throws Exception {
		final Trivial triv3 = new Trivial("foo3", 3);
		final Trivial triv4 = new Trivial("foo4", 3);
		ofy().save().entity(triv3).now();
		ofy().save().entity(triv4).now();

		final List<String> conditions = Arrays.asList("foo3", "foo4", "baz");

		final List<Trivial> result = ofy().load().type(Trivial.class).filter("someString in", conditions).list();
		assertThat(result).containsExactly(triv3, triv4);
	}

	@Test
	void specialKeyFilteringByIN() throws Exception {
		final Trivial triv3 = new Trivial("foo3", 3);
		final Key<Trivial> key3 = ofy().save().entity(triv3).now();
		final Set<Key<Trivial>> singleton = Collections.singleton(key3);

		final List<Trivial> result = ofy().load().type(Trivial.class).filter("__key__ in", singleton).list();
		assertThat(result).containsExactly(triv3);
	}

	@Test
	void testINfilteringWithKeyField() throws Exception {
		factory().register(Employee.class);

		final Key<Employee> bobKey = Key.create(Employee.class, "bob");
		final Employee fred = new Employee("fred", bobKey);

		ofy().save().entity(fred).now();

		final Set<Key<Employee>> singleton = Collections.singleton(bobKey);

		final List<Employee> result = ofy().load().type(Employee.class).filter("manager in", singleton).list();
		assertThat(result).containsExactly(fred);
	}

}
