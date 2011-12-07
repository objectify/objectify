package com.googlecode.objectify.test.entity;

import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Embed;
import com.googlecode.objectify.annotation.Entity;

/**
 */
@Entity
@Cache
public class Person
{
	@Embed public Name name;
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
