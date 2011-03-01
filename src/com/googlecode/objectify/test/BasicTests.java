/*
 * $Id: BeanMixin.java 1075 2009-05-07 06:41:19Z lhoriman $
 * $URL: https://subetha.googlecode.com/svn/branches/resin/rtest/src/org/subethamail/rtest/util/BeanMixin.java $
 */

package com.googlecode.objectify.test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.testng.annotations.Test;

import com.google.appengine.api.datastore.ReadPolicy.Consistency;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyOpts;
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
	private static Logger log = Logger.getLogger(BasicTests.class.getName());

	/** */
	@Test
	public void testGenerateId() throws Exception
	{
		Objectify ofy = this.fact.begin();

		// Note that 5 is not the id, it's part of the payload
		Trivial triv = new Trivial("foo", 5);
		Key<Trivial> k = ofy.put(triv);

		assert k.getKind().equals(triv.getClass().getSimpleName());
		assert k.getId() == triv.getId();

		Key<Trivial> created = new Key<Trivial>(Trivial.class, k.getId());
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
		Objectify ofy = this.fact.begin();

		Trivial triv = new Trivial("foo", 5);
		Key<Trivial> k = ofy.put(triv);

		Trivial triv2 = new Trivial(k.getId(), "bar", 6);
		Key<Trivial> k2 = ofy.put(triv2);

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
		Objectify ofy = this.fact.begin();

		NamedTrivial triv = new NamedTrivial("first", "foo", 5);
		Key<NamedTrivial> k = ofy.put(triv);

		assert k.getName().equals("first");

		Key<NamedTrivial> createdKey = new Key<NamedTrivial>(NamedTrivial.class, "first");
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
		Objectify ofy = this.fact.begin();

		Trivial triv1 = new Trivial("foo", 5);
		Trivial triv2 = new Trivial("foo2", 6);

		List<Trivial> objs = new ArrayList<Trivial>();
		objs.add(triv1);
		objs.add(triv2);

		Map<Key<Trivial>, Trivial> map = ofy.put(objs);
		List<Key<Trivial>> keys = new ArrayList<Key<Trivial>>(map.keySet());

		// Verify the put keys
		assert keys.size() == objs.size();
		for (int i=0; i<objs.size(); i++)
		{
			assert keys.get(i).getId() == objs.get(i).getId();
		}

		// Now fetch and verify the data
		Map<Key<Trivial>, Trivial> fetched = ofy.get(keys);

		assert fetched.size() == keys.size();
		for (Trivial triv: objs)
		{
			Trivial fetchedTriv = fetched.get(this.fact.getKey(triv));
			assert triv.getSomeNumber() == fetchedTriv.getSomeNumber();
			assert triv.getSomeString().equals(fetchedTriv.getSomeString());
		}
	}

	/** */
	@Test
	public void testManyToOne() throws Exception
	{
		Objectify ofy = this.fact.begin();

		Employee fred = new Employee("fred");
		ofy.put(fred);

		Key<Employee> fredKey = this.fact.getKey(fred);

		List<Employee> employees = new ArrayList<Employee>(1100);
		for (int i = 0; i < 1100; i++)
		{
			Employee emp = new Employee("foo" + i, fredKey);
			employees.add(emp);
		}

		ofy.put(employees);

		assert employees.size() == 1100;

		int count = 0;
		for (Employee emp: ofy.query(Employee.class).filter("manager", fred))
		{
			emp.getName(); // Just to make eclipse happy
			count++;
		}
		assert count == 1100;
	}

	/** */
	@Test
	public void testConsistencySetting() throws Exception
	{
		Objectify ofy = this.fact.begin(new ObjectifyOpts().setConsistency(Consistency.EVENTUAL));

		Trivial triv = new Trivial("foo", 5);
		ofy.put(triv);
	}

	/** */
	@Test
	public void testKeyToString() throws Exception
	{
		Key<Trivial> trivKey = new Key<Trivial>(Trivial.class, 123);
		
		String stringified = this.fact.keyToString(trivKey);
		
		Key<Trivial> andBack = this.fact.stringToKey(stringified);
		
		assert trivKey.equals(andBack);
	}
	
	/**
	 * Note that this produces an exception (and thus test failure), but it is the
	 * low-level api that produces the error.  This bug should be fixed in the GAE
	 * code rather than Objectify.
	 */
	@Test
	public void testPutNothing() throws Exception
	{
		Objectify ofy = this.fact.begin();
		
		ofy.put(Collections.emptyList());
	}
}