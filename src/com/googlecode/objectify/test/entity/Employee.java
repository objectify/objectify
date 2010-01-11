/*
 * $Id$ $URL:
 * https://subetha
 * .googlecode.com/svn/branches/resin/rtest/src/org/subethamail/rtest
 * /util/BeanMixin.java $
 */

package com.googlecode.objectify.test.entity;

import javax.persistence.Id;

import com.googlecode.objectify.OKey;
import com.googlecode.objectify.OPreparedQuery;
import com.googlecode.objectify.OQuery;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyService;

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
	public OKey<Employee> manager;

	/** Default constructor must always exist */
	public Employee() {}

	/** set a name */
	public Employee(String name)
	{
		this.name = name;
	}

	/** set a name and manager */
	public Employee(String name, OKey<Employee> manager)
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
		Objectify ofy = ObjectifyService.factory().begin();

		OQuery<Employee> q = ObjectifyService.factory().createQuery(Employee.class);
		q.filter("manager", ObjectifyService.factory().createKey(this));
		OPreparedQuery<Employee> pq = ofy.prepare(q);
		return pq.asIterable();
	}
}