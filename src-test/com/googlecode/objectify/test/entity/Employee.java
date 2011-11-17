/*
 */

package com.googlecode.objectify.test.entity;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;

/**
 * An employee with a key for a Many to one test case.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 * @author Jon Stevens
 */
@Entity
@Cache
public class Employee
{
	@Id
	public String name;
	public Key<Employee> manager;

	/** Default constructor must always exist */
	public Employee() {}

	/** set a name */
	public Employee(String name)
	{
		this.name = name;
	}

	/** set a name and manager */
	public Employee(String name, Key<Employee> manager)
	{
		this.name = name;
		this.manager = manager;
	}

	/** */
	public String getName()
	{
		return this.name;
	}
}