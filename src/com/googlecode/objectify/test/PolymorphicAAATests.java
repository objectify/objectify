/*
 * $Id: BeanMixin.java 1075 2009-05-07 06:41:19Z lhoriman $
 * $URL: https://subetha.googlecode.com/svn/branches/resin/rtest/src/org/subethamail/rtest/util/BeanMixin.java $
 */

package com.googlecode.objectify.test;

import java.util.logging.Logger;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.testng.annotations.Test;

import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.annotation.Subclass;

/**
 * Just the registration part of polymorphic classes.  The 'A' just to alphabetize it before
 * the other polymorphic tests.
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class PolymorphicAAATests extends TestBase
{
	/** */
	@SuppressWarnings("unused")
	private static Logger log = Logger.getLogger(PolymorphicAAATests.class.getName());

	/** */
	@Entity
	public static class Animal
	{
		@Id Long id;
		String name;
	}
	
	/** */
	@Subclass
	public static class Mammal extends Animal
	{
		boolean longHair;
	}
	
	/** */
	@Subclass
	public static class Cat extends Mammal
	{
		boolean hypoallergenic;
	}
	
	/** */
	@Subclass(unindexed=true)
	public static class Dog extends Mammal
	{
		int loudness;
	}
	
	/** */
	@Test
	public void testRegistrationForwards() throws Exception
	{
		this.fact.register(Animal.class);
		this.fact.register(Mammal.class);
		this.fact.register(Cat.class);
		this.fact.register(Dog.class);
	}

	/** */
	@Test
	public void testRegistrationBackwards() throws Exception
	{
		this.fact.register(Dog.class);
		this.fact.register(Cat.class);
		this.fact.register(Mammal.class);
		this.fact.register(Animal.class);
	}

	/** */
	@Test
	public void testBasicFetch() throws Exception
	{
		this.testRegistrationForwards();
		
		Animal a = new Animal();
		a.name = "Bob";
		Animal a2 = this.putAndGet(a);
		assert a.name.equals(a2.name);
		
		Mammal m = new Mammal();
		m.name = "Bob";
		m.longHair = true;
		Mammal m2 = this.putAndGet(m);
		assert m.name.equals(m2.name);
		assert m.longHair == m2.longHair;

		Cat c = new Cat();
		c.name = "Bob";
		c.longHair = true;
		c.hypoallergenic = true;
		Cat c2 = this.putAndGet(c);
		assert c.name.equals(c2.name);
		assert c.longHair == c2.longHair;
		assert c.hypoallergenic == c2.hypoallergenic;
	}

	/** 
	 * Issue #80:  http://code.google.com/p/objectify-appengine/issues/detail?id=80
	 */
	@Test
	public void testNullFind() throws Exception
	{
		this.testRegistrationForwards();
		
		Objectify ofy = this.fact.begin();
		
		// This should produce null
		Cat cat = ofy.find(Cat.class, 123);
		
		assert cat == null;
	}

	/**
	 * This seems reasonable behavior, better than filtering mismatched values out of the result.
	 * We like this behavior better because it provides a more clear explanation to the user of
	 * what went wrong - the CCE is pretty explicit about the classes involved.  If we returned
	 * null folks would think the data wasn't in the db.
	 */
	@Test(expectedExceptions=ClassCastException.class)
	public void testFetchMismatch() throws Exception
	{
		this.testRegistrationForwards();
		
		Objectify ofy = this.fact.begin();
		
		Animal a = new Animal();
		a.name = "Bob";
		ofy.put(a);
		
		// This should exclude the value
		@SuppressWarnings("unused")
		Mammal m = ofy.find(Mammal.class, a.id);
	}
}
