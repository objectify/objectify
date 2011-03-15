/*
 * $Id: BeanMixin.java 1075 2009-05-07 06:41:19Z lhoriman $
 * $URL: https://subetha.googlecode.com/svn/branches/resin/rtest/src/org/subethamail/rtest/util/BeanMixin.java $
 */

package com.googlecode.objectify.test;

import java.util.List;
import java.util.logging.Logger;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.test.PolymorphicAAATests.Animal;
import com.googlecode.objectify.test.PolymorphicAAATests.Cat;
import com.googlecode.objectify.test.PolymorphicAAATests.Dog;
import com.googlecode.objectify.test.PolymorphicAAATests.Mammal;

/**
 * Tests of polymorphic persistence and queries
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class PolymorphicQueryTests extends TestBase
{
	/** */
	@SuppressWarnings("unused")
	private static Logger log = Logger.getLogger(PolymorphicQueryTests.class.getName());

	/** */
	Animal animal;
	Mammal mammal;
	Cat cat;
	Dog dog;
	
	/** */
	@BeforeMethod
	public void setUp()
	{
		super.setUp();
		
		this.fact.register(Animal.class);
		this.fact.register(Mammal.class);
		this.fact.register(Cat.class);
		this.fact.register(Dog.class);
		
		Objectify ofy = this.fact.begin();
		
		this.animal = new Animal();
		this.animal.name = "Ann";
		ofy.put(this.animal);
		
		this.mammal = new Mammal();
		this.mammal.name = "Mamet";
		this.mammal.longHair = true;
		ofy.put(this.mammal);
		
		this.cat = new Cat();
		this.cat.name = "Catrina";
		this.cat.longHair = true;
		this.cat.hypoallergenic = true;
		ofy.put(this.cat);
		
		this.dog = new Dog();
		this.dog.name = "Doug";
		this.dog.longHair = true;
		this.dog.loudness = 11;
		ofy.put(this.dog);
	}
	
	/** */
	@Test
	public void testQueryAll() throws Exception
	{
		Objectify ofy = this.fact.begin();
		
		List<Animal> all = ofy.query(Animal.class).list();
		assert all.size() == 4;
		
		Animal ann = all.get(0);
		assert ann.name.equals(this.animal.name);
		
		Mammal mamet = (Mammal)all.get(1);
		assert mamet.longHair == this.mammal.longHair;
		
		Cat catrina = (Cat)all.get(2);
		assert catrina.hypoallergenic == this.cat.hypoallergenic;

		Dog doug = (Dog)all.get(3);
		assert doug.loudness == this.dog.loudness;
	}

	/** */
	@Test
	public void testQueryMammal() throws Exception
	{
		Objectify ofy = this.fact.begin();
		
		List<Mammal> all = ofy.query(Mammal.class).list();
		assert all.size() == 3;
		
		Mammal mamet = (Mammal)all.get(0);
		assert mamet.longHair == this.mammal.longHair;
		
		Cat catrina = (Cat)all.get(1);
		assert catrina.hypoallergenic == this.cat.hypoallergenic;

		Dog doug = (Dog)all.get(2);
		assert doug.loudness == this.dog.loudness;
	}

	/** */
	@Test
	public void testQueryCat() throws Exception
	{
		Objectify ofy = this.fact.begin();
		
		List<Cat> all = ofy.query(Cat.class).list();
		assert all.size() == 1;
		
		Cat catrina = (Cat)all.get(0);
		assert catrina.hypoallergenic == this.cat.hypoallergenic;
	}

	/** Dog class is unindexed, but property is indexed */
	@Test
	public void testQueryWithUnindexedPoly() throws Exception
	{
		Objectify ofy = this.fact.begin();
		
		List<Dog> dogs = ofy.query(Dog.class).list();
		assert dogs.size() == 0;

		List<Animal> loud = ofy.query(Animal.class).filter("loudness", this.dog.loudness).list();
		assert loud.size() == 1;
		
		Dog doug = (Dog)loud.get(0);
		assert doug.loudness == this.dog.loudness;

		// Let's try that again with a mammal query
		List<Mammal> mloud = ofy.query(Mammal.class).filter("loudness", this.dog.loudness).list();
		assert mloud.size() == 1;
		
		doug = (Dog)mloud.get(0);
		assert doug.loudness == this.dog.loudness;
		
	}
	
	/** */
	@Test
	public void testFilterOnProperty() throws Exception
	{
		Objectify ofy = this.fact.begin();
		
		Cat other = new Cat();
		other.name = "OtherCat";
		other.hypoallergenic = false;
		other.longHair = false;
		
		ofy.put(other);
		
		// now query, should only get Catrina
		List<Cat> cats = ofy.query(Cat.class).filter("longHair", true).list();
		assert cats.size() == 1;
		assert cats.get(0).name.equals(this.cat.name);
	}
}
