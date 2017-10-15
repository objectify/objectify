/*
 */

package com.googlecode.objectify.test.entity;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.Load;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * An employee with a key for a Many to one test case.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 * @author Jon Stevens
 */
@Entity
@Cache
@Data
@NoArgsConstructor
public class Employee {
	@Id
	private String name;

	@Index
	private Key<Employee> manager;

	@Index @Load
	private Ref<Employee> manager2;

	public Employee(String name)
	{
		this.name = name;
	}

	public Employee(String name, Key<Employee> manager) {
		this.name = name;
		this.manager = manager;
	}

	public Employee(String name, Employee manager2) {
		this.name = name;
		this.manager2 = Ref.create(manager2);
	}
}