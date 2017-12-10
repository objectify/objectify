/*
 */

package com.googlecode.objectify.test;

import com.google.cloud.datastore.StructuredQuery.Filter;
import com.google.cloud.datastore.StructuredQuery.PropertyFilter;
import com.google.common.collect.Lists;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.LoadResult;
import com.googlecode.objectify.cmd.Query;
import com.googlecode.objectify.test.entity.Child;
import com.googlecode.objectify.test.entity.Employee;
import com.googlecode.objectify.test.entity.Trivial;
import com.googlecode.objectify.test.util.TestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.truth.Truth.assertThat;
import static com.googlecode.objectify.ObjectifyService.factory;
import static com.googlecode.objectify.ObjectifyService.ofy;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests of various queries
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
class QueryTests extends TestBase {

	/** */
	private Trivial triv1;
	private Trivial triv2;
	private List<Key<Trivial>> keys;

	/** */
	@BeforeEach
	void setUpExtra() {
		factory().register(Trivial.class);

		this.triv1 = new Trivial("foo1", 1);
		this.triv2 = new Trivial("foo2", 2);

		final Map<Key<Trivial>, Trivial> result = ofy().save().entities(triv1, triv2).now();

		this.keys = new ArrayList<>(result.keySet());
	}

	/** */
	@Test
	void keysOnly() throws Exception {
		final Query<Trivial> q = ofy().load().type(Trivial.class);

		assertThat(q.keys()).containsExactlyElementsIn(this.keys);

		// Just for the hell of it, test the other methods
		assertThat(q.count()).isEqualTo(keys.size());

		assertThat(q.limit(1).keys()).containsExactly(this.keys.get(0));

		assertThat(q.keys().first().now()).isEqualTo(this.keys.get(0));

		assertThat(q.offset(1).keys().first().now()).isEqualTo(this.keys.get(1));
	}

	/** */
	@Test
	void normalSorting() throws Exception {
		final List<Trivial> list = ofy().load().type(Trivial.class).order("someString").list();
		assertThat(list).containsExactly(triv1, triv2).inOrder();
	}

	/** */
	@Test
	void normalReverseSorting() throws Exception {
		final List<Trivial> list = ofy().load().type(Trivial.class).order("-someString").list();
		assertThat(list).containsExactly(triv2, triv1).inOrder();
	}

	/** */
	@Test
	void keySorting() throws Exception {
		final List<Trivial> list = ofy().load().type(Trivial.class).orderKey(false).list();
		assertThat(list).containsExactly(triv1, triv2).inOrder();
	}

	/** */
	@Test
	void testKeyReverseSorting() throws Exception {
		final List<Trivial> list = ofy().load().type(Trivial.class).orderKey(true).list();
		assertThat(list).containsExactly(triv2, triv1).inOrder();
	}

	/** Unfortunately we can only test one way without custom index file */
	@Test
	void doNotAllowSortingByIdField() throws Exception {
		assertThrows(IllegalArgumentException.class, () -> ofy().load().type(Trivial.class).order("id").iterator());
	}

	/** */
	@Test
	void filtering() throws Exception {
		final Iterator<Trivial> it = ofy().load().type(Trivial.class).filter("someString >", triv1.getSomeString()).iterator();
		final ArrayList<Trivial> list = Lists.newArrayList(it);

		assertThat(list).containsExactly(triv2);
	}

	/** */
	@Test
	void filteringByNull() throws Exception {
		final Trivial triv3 = new Trivial(null, 3);
		ofy().save().entity(triv3).now();

		final List<Trivial> list = ofy().load().type(Trivial.class).filter("someString", null).list();
		assertThat(list).containsExactly(triv3);
	}

	/** */
	@Test
	void doNotAllowFilteringOnIdProperties() throws Exception {
		assertThrows(IllegalArgumentException.class, () -> ofy().load().type(Trivial.class).filter("id >", triv1.getId()).iterator());
	}

	/** */
	@Test
	void filterWithLowLevelFilterObject() throws Exception {
		final Filter filter = PropertyFilter.gt("someString", triv1.getSomeString());

		final List<Trivial> list = ofy().load().type(Trivial.class).filter(filter).list();
		assertThat(list).containsExactly(triv2);
	}

	/** */
	@Test
	void queryToStringIsUnique() throws Exception {
		final Query<Trivial> q1 = ofy().load().type(Trivial.class).filter("someString >", "blah");
		final Query<Trivial> q2 = ofy().load().type(Trivial.class).filter("someString <", "blah");
		final Query<Trivial> q3 = ofy().load().type(Trivial.class).filter("someString >", "blah").order("-__key__");

		assertThat(q1.toString()).isNotEqualTo(q2.toString());
		assertThat(q2.toString()).isNotEqualTo(q3.toString());
		assertThat(q3.toString()).isNotEqualTo(q1.toString());
	}

	/** */
	@Test
	void emptySingleResult() throws Exception {
		final Query<Trivial> q = ofy().load().type(Trivial.class).filter("someString", "nada");	// no such entity
		final LoadResult<Trivial> res = q.first();
		assertThat(res.now()).isNull();
	}

	/** */
	@Test
	void filteringByKeyField() throws Exception {
		factory().register(Employee.class);

		final Key<Employee> bobKey = Key.create(Employee.class, "bob");

		final Employee fred = new Employee("fred", bobKey);
		ofy().save().entity(fred).now();

		final List<Employee> list = ofy().load().type(Employee.class).filter("manager", bobKey).list();
		assertThat(list).containsExactly(fred);
	}

	/** */
	@Test
	void filteringByEntityField() throws Exception {
		factory().register(Employee.class);

		final Employee bob = new Employee("bob");
		ofy().save().entity(bob).now();

		final Employee fred = new Employee("fred", bob);
		ofy().save().entity(fred).now();

		final List<Employee> list = ofy().load().type(Employee.class).filter("manager2", bob).list();
		assertThat(list).containsExactly(fred);
	}

	/** */
	@Test
	void filteringByAncestor() throws Exception {
		factory().register(Child.class);

		final Trivial triv = new Trivial(null, 3);
		final Key<Trivial> trivKey = ofy().save().entity(triv).now();

		final Child child = new Child(trivKey, "blah");
		ofy().save().entity(child).now();

		ofy().clear();	// why not
		final List<Object> list = ofy().load().ancestor(trivKey).list();
		assertThat(list).containsExactly(triv, child).inOrder();
	}

	/** No longer supported by the SDK */
	//@Test
	void testIN() throws Exception {
		final Trivial triv1 = new Trivial("foo", 3);
		final Trivial triv2 = new Trivial("bar", 3);
		ofy().save().entity(triv1).now();
		ofy().save().entity(triv2).now();

		final List<String> conditions = Arrays.asList("foo", "bar", "baz");

		final List<Trivial> result = ofy().load().type(Trivial.class).filter("someString in", conditions).list();
		assertThat(result).containsExactly(triv1, triv2);
	}

	/** IN no longer supported by SDK */
	//@Test
	void specialKeyFilteringByIN() throws Exception {
		final Trivial triv1 = new Trivial("foo", 3);
		final Key<Trivial> key1 = ofy().save().entity(triv1).now();
		final Set<Key<Trivial>> singleton = Collections.singleton(key1);

		final List<Trivial> result = ofy().load().type(Trivial.class).filter("__key__ in", singleton).list();
		assertThat(result).containsExactly(triv1);
	}

	/** IN no longer supported by SDK */
	//@Test
	void testINfilteringWithKeyField() throws Exception {
		factory().register(Employee.class);

		final Key<Employee> bobKey = Key.create(Employee.class, "bob");
		final Employee fred = new Employee("fred", bobKey);

		ofy().save().entity(fred).now();

		final Set<Key<Employee>> singleton = Collections.singleton(bobKey);

		final List<Employee> result = ofy().load().type(Employee.class).filter("manager in", singleton).list();
		assertThat(result).containsExactly(fred);
	}

	/** */
	@Test
	void countWorks() throws Exception {
		final int count = ofy().load().type(Trivial.class).count();
		assertThat(count).isEqualTo(2);
	}

	/** */
	@Test
	void limitWorks() throws Exception {
		final List<Trivial> trivs = ofy().load().type(Trivial.class).limit(1).list();
		assertThat(trivs).containsExactly(triv1);
	}

	/** */
	@Test
	void queryForNonRegisteredEntity() throws Exception {
		assertThrows(IllegalArgumentException.class, () -> ofy().load().type(Employee.class).limit(1).list());
	}
}
