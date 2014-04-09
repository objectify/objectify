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

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static com.googlecode.objectify.test.util.TestObjectifyService.fact;
import static com.googlecode.objectify.test.util.TestObjectifyService.ofy;

/**
 * Just the registration part of polymorphic classes.  The 'A' just to alphabetize it before
 * the other polymorphic tests.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class EmbeddedPolymorphismTests extends TestBase
{
	/** */
	@SuppressWarnings("unused")
	private static Logger log = Logger.getLogger(EmbeddedPolymorphismTests.class.getName());

	/** */
	@Entity
	public static class Handler {
		@Id Long id;
		Animal animal;

		public Handler() {}
		public Handler(Animal animal) {
			this.animal = animal;
		}
	}

	@Index
	public static class Animal {
		String name;

		public Animal() {}
		public Animal(String name) {
			this.name = name;
		}
	}

	/** */
	@Subclass
	@Index
	public static class Mammal extends Animal {
		boolean longHair;

		public Mammal() {}
		public Mammal(String name, boolean longHair) {
			super(name);
			this.longHair = longHair;
		}
	}

	/** */
	@Subclass
	@Index
	public static class Cat extends Mammal {
		boolean hypoallergenic;

		public Cat() {}
		public Cat(String name, boolean longHair, boolean hypoallergenic) {
			super(name, longHair);
			this.hypoallergenic = hypoallergenic;
		}
	}

	/** */
	@Subclass
	@Index
	public static class Dog extends Mammal {
		int loudness;

		public Dog() {}
		public Dog(String name, boolean longHair, int loudness) {
			super(name, longHair);
			this.loudness = loudness;
		}
	}

	/** */
	@Test
	public void testRegistrationForwards() throws Exception {
		fact().register(Handler.class);

		fact().register(Mammal.class);
		fact().register(Cat.class);
		fact().register(Dog.class);
	}

	/** */
	@Test
	public void testRegistrationBackwards() throws Exception {
		fact().register(Dog.class);
		fact().register(Cat.class);
		fact().register(Mammal.class);

		fact().register(Handler.class);
	}

	/** */
	@Test
	public void embeddedBaseClassWorks() throws Exception {
		this.testRegistrationForwards();

		Handler handler = new Handler(new Animal("Bob"));
		Handler fetched = ofy().saveClearLoad(handler);
		assert handler.animal.name.equals(fetched.animal.name);
	}

	/** */
	@Test
	public void firstSubclassWorks() throws Exception {
		this.testRegistrationForwards();

		Handler handler = new Handler(new Mammal("Bob", true));
		Handler fetched = ofy().saveClearLoad(handler);
		assert handler.animal.name.equals(fetched.animal.name);
		assert ((Mammal)handler.animal).longHair == ((Mammal)fetched.animal).longHair;
	}

	/** */
	@Test
	public void secondSubclassWorks() throws Exception {
		this.testRegistrationBackwards();

		Handler handler = new Handler(new Cat("Bob", true, true));
		Handler fetched = ofy().saveClearLoad(handler);
		assert handler.animal.name.equals(fetched.animal.name);
		assert ((Mammal)handler.animal).longHair == ((Mammal)fetched.animal).longHair;
		assert ((Cat)handler.animal).hypoallergenic == ((Cat)fetched.animal).hypoallergenic;
	}

	/** */
	@Test
	public void queryingOnIndexedPropertiesWorks() throws Exception {
		this.testRegistrationForwards();

		Handler handler = new Handler(new Cat("Bob", true, true));
		ofy().save().entity(handler).now();

		Handler fetched = ofy().load().type(Handler.class).filter("animal.hypoallergenic", true).first().now();

		assert handler.animal.name.equals(fetched.animal.name);
		assert ((Mammal)handler.animal).longHair == ((Mammal)fetched.animal).longHair;
		assert ((Cat)handler.animal).hypoallergenic == ((Cat)fetched.animal).hypoallergenic;
	}

	/** */
	@Entity
	public static class BusyHandler {
		public @Id Long id;
		public List<Animal> animals = new ArrayList<>();
	}

	/** */
	@Test
	public void collectionOfPolymorphismWorks() throws Exception {
		this.testRegistrationForwards();
		fact().register(BusyHandler.class);

		BusyHandler handler = new BusyHandler();
		handler.animals.add(new Animal("Bob"));
		handler.animals.add(new Mammal("Bob", true));
		handler.animals.add(new Cat("Bob", true, true));

		BusyHandler fetched = ofy().saveClearLoad(handler);
		assert fetched.animals.size() == 3;
		assert handler.animals.get(0).name.equals(fetched.animals.get(0).name);
		assert ((Mammal)handler.animals.get(1)).longHair == ((Mammal)fetched.animals.get(1)).longHair;
		assert ((Cat)handler.animals.get(2)).hypoallergenic == ((Cat)fetched.animals.get(2)).hypoallergenic;
	}

	/** */
	@Test
	public void queryingOnCollectionWorks() throws Exception {
		this.testRegistrationForwards();
		fact().register(BusyHandler.class);

		BusyHandler handler = new BusyHandler();
		handler.animals.add(new Animal("Bob"));
		handler.animals.add(new Mammal("Bob", true));
		ofy().save().entity(handler).now();

		BusyHandler handler2 = new BusyHandler();
		handler2.animals.add(new Mammal("Bob", true));
		handler2.animals.add(new Cat("Bob", true, true));
		ofy().save().entity(handler2).now();


		List<BusyHandler> both = ofy().load().type(BusyHandler.class).filter("animals.longHair", true).list();
		assert both.size() == 2;

		List<BusyHandler> second = ofy().load().type(BusyHandler.class).filter("animals.hypoallergenic", true).list();
		assert second.size() == 1;
		assert second.get(0).id == handler2.id;
	}

}
