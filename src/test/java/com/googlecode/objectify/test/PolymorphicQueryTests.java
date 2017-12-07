/*
 */

package com.googlecode.objectify.test;

import com.googlecode.objectify.test.PolymorphicAAATests.Animal;
import com.googlecode.objectify.test.PolymorphicAAATests.Cat;
import com.googlecode.objectify.test.PolymorphicAAATests.Dog;
import com.googlecode.objectify.test.PolymorphicAAATests.Mammal;
import com.googlecode.objectify.test.util.TestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static com.googlecode.objectify.ObjectifyService.factory;
import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * Tests of polymorphic persistence and queries
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
class PolymorphicQueryTests extends TestBase
{
	/** */
	private Animal animal;
	private Mammal mammal;
	private Cat cat;
	private Dog dog;

	/** */
	@BeforeEach
	void setUpExtra() {
		factory().register(Animal.class);
		factory().register(Mammal.class);
		factory().register(Cat.class);
		factory().register(Dog.class);

		this.animal = new Animal();
		this.animal.name = "Ann";
		ofy().save().entity(this.animal).now();

		this.mammal = new Mammal();
		this.mammal.name = "Mamet";
		this.mammal.longHair = true;
		ofy().save().entity(this.mammal).now();

		this.cat = new Cat();
		this.cat.name = "Catrina";
		this.cat.longHair = true;
		this.cat.hypoallergenic = true;
		ofy().save().entity(this.cat).now();

		this.dog = new Dog();
		this.dog.name = "Doug";
		this.dog.longHair = true;
		this.dog.loudness = 11;
		ofy().save().entity(this.dog).now();
	}

	/** */
	@Test
	void queryAll() throws Exception {
		final List<Animal> all = ofy().load().type(Animal.class).list();
		assertThat(all).containsExactly(animal, mammal, cat, dog);
	}

	/** */
	@Test
	void queryMammal() throws Exception {
		final List<Mammal> all = ofy().load().type(Mammal.class).list();
		assertThat(all).containsExactly(mammal, cat, dog);
	}

	/** */
	@Test
	void queryCat() throws Exception {
		final List<Cat> all = ofy().load().type(Cat.class).list();
		assertThat(all).containsExactly(cat);
	}

	/** Dog class is unindexed, but property is indexed */
	@Test
	void queryWithUnindexedPoly() throws Exception {
		final List<Dog> dogs = ofy().load().type(Dog.class).list();
		assertThat(dogs).isEmpty();

		ofy().clear();
		final List<Animal> loud = ofy().load().type(Animal.class).filter("loudness", this.dog.loudness).list();
		assertThat(loud).containsExactly(dog);

		ofy().clear();
		// Let's try that again with a mammal query
		final List<Mammal> mloud = ofy().load().type(Mammal.class).filter("loudness", this.dog.loudness).list();
		assertThat(loud).containsExactly(dog);
	}

	/** */
	@Test
	void filterOnProperty() throws Exception {
		final Cat other = new Cat();
		other.name = "OtherCat";
		other.hypoallergenic = false;
		other.longHair = false;

		ofy().save().entity(other).now();
		ofy().clear();

		final List<Cat> cats = ofy().load().type(Cat.class).filter("longHair", true).list();
		assertThat(cats).containsExactly(cat);
	}
}
