package com.googlecode.objectify.test.entity;

import javax.persistence.Embedded;

/**
 */
public class Person
{
	@Embedded
	public Name name;
	public int age;

	public Person()
	{
	}

	public Person(Name name, int age)
	{
		this.name = name;
		this.age = age;
	}
}
