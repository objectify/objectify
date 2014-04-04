/*
 * $Id: BeanMixin.java 1075 2009-05-07 06:41:19Z lhoriman $
 * $URL: https://subetha.googlecode.com/svn/branches/resin/rtest/src/org/subethamail/rtest/util/BeanMixin.java $
 */

package com.googlecode.objectify.test;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.Subclass;
import com.googlecode.objectify.test.util.TestBase;
import org.testng.annotations.Test;

import java.util.logging.Logger;

import static com.googlecode.objectify.test.util.TestObjectifyService.fact;
import static com.googlecode.objectify.test.util.TestObjectifyService.ofy;

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
	@Index
	public static class Animal
	{
		@Id Long id;
		String name;
	}

	/** */
	@Subclass(index=true)
	@Index
	public static class Mammal extends Animal
	{
		boolean longHair;
	}

	/** */
	@Subclass(index=true)
	@Index
	public static class Cat extends Mammal
	{
		boolean hypoallergenic;
	}

	/** */
	@Subclass(index=false)
	@Index
	public static class Dog extends Mammal
	{
		int loudness;
	}

	/** */
	@Test
	public void testRegistrationForwards() throws Exception
	{
		fact().register(Animal.class);
		fact().register(Mammal.class);
		fact().register(Cat.class);
		fact().register(Dog.class);
	}

	/** */
	@Test
	public void testRegistrationBackwards() throws Exception
	{
		fact().register(Dog.class);
		fact().register(Cat.class);
		fact().register(Mammal.class);
		fact().register(Animal.class);
	}

	/** */
	@Test
	public void testBasicFetch() throws Exception
	{
		this.testRegistrationForwards();

		Animal a = new Animal();
		a.name = "Bob";
		Animal a2 = ofy().putClearGet(a);
		assert a.name.equals(a2.name);

		Mammal m = new Mammal();
		m.name = "Bob";
		m.longHair = true;
		Mammal m2 = ofy().putClearGet(m);
		assert m.name.equals(m2.name);
		assert m.longHair == m2.longHair;

		Cat c = new Cat();
		c.name = "Bob";
		c.longHair = true;
		c.hypoallergenic = true;
		Cat c2 = ofy().putClearGet(c);
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

		// This should produce null
		Cat cat = ofy().load().type(Cat.class).id(123).now();

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

		Animal a = new Animal();
		a.name = "Bob";
		ofy().save().entity(a).now();

		// This should exclude the value
		@SuppressWarnings("unused")
		Mammal m = ofy().load().type(Mammal.class).id(a.id).now();
	}
}
