/*
 */

package com.googlecode.objectify.test;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.Subclass;
import com.googlecode.objectify.test.util.TestBase;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static com.googlecode.objectify.ObjectifyService.factory;
import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * Just the registration part of polymorphic classes.  The 'A' just to alphabetize it before
 * the other polymorphic tests.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
class PolymorphicAAATests extends TestBase {

	/** */
	@Entity
	@Index
	@Data
	public static class Animal {
		@Id Long id;
		String name;
	}

	/** */
	@Subclass(index=true)
	@Index
	@Data
	@EqualsAndHashCode(callSuper = true)
	public static class Mammal extends Animal {
		boolean longHair;
	}

	/** */
	@Subclass(index=true)
	@Index
	@Data
	@EqualsAndHashCode(callSuper = true)
	public static class Cat extends Mammal {
		boolean hypoallergenic;
	}

	/** */
	@Subclass(index=false)
	@Index
	@Data
	@EqualsAndHashCode(callSuper = true)
	public static class Dog extends Mammal {
		int loudness;
	}

	/** */
	@Test
	void registrationForwards() throws Exception {
		factory().register(Animal.class);
		factory().register(Mammal.class);
		factory().register(Cat.class);
		factory().register(Dog.class);
	}

	/** */
	@Test
	void registrationBackwards() throws Exception {
		factory().register(Dog.class);
		factory().register(Cat.class);
		factory().register(Mammal.class);
		factory().register(Animal.class);
	}

	/** */
	@Test
	void basicFetch() throws Exception {
		this.registrationForwards();

		final Animal a = new Animal();
		a.name = "Bob";
		final Animal a2 = saveClearLoad(a);
		assertThat(a2).isEqualTo(a);

		final Mammal m = new Mammal();
		m.name = "Bob";
		m.longHair = true;
		final Mammal m2 = saveClearLoad(m);
		assertThat(m2).isEqualTo(m);

		final Cat c = new Cat();
		c.name = "Bob";
		c.longHair = true;
		c.hypoallergenic = true;
		final Cat c2 = saveClearLoad(c);
		assertThat(c2).isEqualTo(c);
	}

	/**
	 * Issue #80:  http://code.google.com/p/objectify-appengine/issues/detail?id=80
	 */
	@Test
	void nullFind() throws Exception {
		this.registrationForwards();

		// This should produce null
		final Cat cat = ofy().load().type(Cat.class).id(123).now();

		assertThat(cat).isNull();
	}

	/**
	 * This seems reasonable behavior, better than filtering mismatched values out of the result.
	 * We like this behavior better because it provides a more clear explanation to the user of
	 * what went wrong - the CCE is pretty explicit about the classes involved.  If we returned
	 * null folks would think the data wasn't in the db.
	 */
	@Test
	void fetchingDowncastedKeyStillReturnsNormalEntity() throws Exception {
		this.registrationForwards();

		final Animal a = new Animal();
		a.name = "Bob";
		ofy().save().entity(a).now();

		ofy().clear();

		final Animal fetched = ofy().load().type(Mammal.class).id(a.id).now();
		assertThat(fetched.getClass()).isEqualTo(Animal.class);	// not Mammal.class
	}

	/**
	 */
	@Test
	void keyCreationFindsBaseKind() throws Exception {
		Key<?> key = Key.create(Mammal.class, 123L);

		assertThat(key.getKind()).isEqualTo("Animal");
	}
}
