/*
 * $Id$ $URL:
 * https://subetha
 * .googlecode.com/svn/branches/resin/rtest/src/org/subethamail/rtest
 * /util/BeanMixin.java $
 */

package com.googlecode.objectify.test.entity;

import javax.persistence.Id;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.googlecode.objectify.ObjPreparedQuery;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyFactory;

/**
 * An employee with a key for a Many to one test case.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 * @author Jon Stevens
 */
public class Employee
{
	@Id
	public String name;
	public Key manager;

	/** Default constructor must always exist */
	public Employee() {}

	/** set a name */
	public Employee(String name)
	{
		this.name = name;
	}

	/** set a name and manager */
	public Employee(String name, Key manager)
	{
		this.name = name;
		this.manager = manager;
	}

	/** */
	public String getName()
	{
		return this.name;
	}

	/** */
	public Iterable<Employee> getSubordinates()
 	{
    	Objectify ofy = ObjectifyFactory.begin();

		Query q = ObjectifyFactory.createQuery(Employee.class);
		q.addFilter("manager", FilterOperator.EQUAL, ObjectifyFactory.createKey(this));
		ObjPreparedQuery<Employee> pq = ofy.prepare(q);
		return pq.asIterable();
	}
}