/*
 * $Id: BeanMixin.java 1075 2009-05-07 06:41:19Z lhoriman $
 * $URL: https://subetha.googlecode.com/svn/branches/resin/rtest/src/org/subethamail/rtest/util/BeanMixin.java $
 */

package com.googlecode.objectify.test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import com.googlecode.objectify.ObKey;
import com.googlecode.objectify.ObPreparedQuery;
import com.googlecode.objectify.ObQuery;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.test.entity.Employee;
import com.googlecode.objectify.test.entity.NamedTrivial;
import com.googlecode.objectify.test.entity.Trivial;

/**
 * Tests of basic entity manipulation.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class BasicTests extends TestBase
{
	/** */
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(BasicTests.class);

	/** */
	@Test
	public void testGenerateId() throws Exception
	{
		Objectify ofy = ObjectifyFactory.begin();

		// Note that 5 is not the id, it's part of the payload
		Trivial triv = new Trivial("foo", 5);
		ObKey<Trivial> k = ofy.put(triv);

		assert k.getKind().equals(triv.getClass());
		assert k.getId() == triv.getId();

		ObKey<Trivial> created = ObjectifyFactory.createKey(Trivial.class, k.getId());
		assert k.equals(created);

		Trivial fetched = ofy.get(k);

		assert fetched.getId().equals(k.getId());
		assert fetched.getSomeNumber() == triv.getSomeNumber();
		assert fetched.getSomeString().equals(triv.getSomeString());
	}

	/** */
	@Test
	public void testOverwriteId() throws Exception
	{
		Objectify ofy = ObjectifyFactory.begin();

		Trivial triv = new Trivial("foo", 5);
		ObKey<Trivial> k = ofy.put(triv);

		Trivial triv2 = new Trivial(k.getId(), "bar", 6);
		ObKey<Trivial> k2 = ofy.put(triv2);

		assert k2.equals(k);

		Trivial fetched = ofy.get(k);

		assert fetched.getId() == k.getId();
		assert fetched.getSomeNumber() == triv2.getSomeNumber();
		assert fetched.getSomeString().equals(triv2.getSomeString());
	}

	/** */
	@Test
	public void testNames() throws Exception
	{
		Objectify ofy = ObjectifyFactory.begin();

		NamedTrivial triv = new NamedTrivial("first", "foo", 5);
		ObKey<NamedTrivial> k = ofy.put(triv);

		assert k.getName().equals("first");

		ObKey<NamedTrivial> createdKey = ObjectifyFactory.createKey(NamedTrivial.class, "first");
		assert k.equals(createdKey);

		NamedTrivial fetched = ofy.get(k);

		assert fetched.getName().equals(k.getName());
		assert fetched.getSomeNumber() == triv.getSomeNumber();
		assert fetched.getSomeString().equals(triv.getSomeString());
	}

	/** */
	@Test
	public void testBatchOperations() throws Exception
	{
		Objectify ofy = ObjectifyFactory.begin();

		Trivial triv1 = new Trivial("foo", 5);
		Trivial triv2 = new Trivial("foo2", 6);

		List<Trivial> objs = new ArrayList<Trivial>();
		objs.add(triv1);
		objs.add(triv2);

		List<ObKey<Trivial>> keys = ofy.put(objs);

		// Verify the put keys
		assert keys.size() == objs.size();
		for (int i=0; i<objs.size(); i++)
		{
			assert keys.get(i).getId() == objs.get(i).getId();
		}

		// Now fetch and verify the data
		Map<ObKey<Trivial>, Trivial> fetched = ofy.get(keys);

		assert fetched.size() == keys.size();
		for (Trivial triv: objs)
		{
			Trivial fetchedTriv = fetched.get(ObjectifyFactory.createKey(triv));
			assert triv.getSomeNumber() == fetchedTriv.getSomeNumber();
			assert triv.getSomeString().equals(fetchedTriv.getSomeString());
		}
	}

	/** */
	@Test
	public void testManyToOne() throws Exception
	{
		Objectify ofy = ObjectifyFactory.begin();

		Employee fred = new Employee("fred");
		ofy.put(fred);

		ObKey<Employee> fredKey = ObjectifyFactory.createKey(fred);

		List<Employee> employees = new ArrayList<Employee>(1100);
		for (int i = 0; i < 1100; i++)
		{
			Employee emp = new Employee("foo" + i, fredKey);
			employees.add(emp);
		}

		ofy.put(employees);

		assert employees.size() == 1100;

		ObQuery q = ObjectifyFactory.createQuery(Employee.class);
		q.filter("manager", ObjectifyFactory.createKey(fred));
		ObPreparedQuery<Employee> pq = ofy.prepare(q);
		Iterable<Employee> results = pq.asIterable();

		int count = 0;
		for (Employee emp : results)
		{
			emp.getName(); // Just to make eclipse happy
			count++;
		}
		assert count == 1100;
	}

}