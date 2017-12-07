/*
 */

package com.googlecode.objectify.test;

import com.google.cloud.datastore.FullEntity;
import com.google.cloud.datastore.StringValue;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.Subclass;
import com.googlecode.objectify.test.util.TestBase;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static com.googlecode.objectify.ObjectifyService.factory;
import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * Just the registration part of polymorphic classes.  The 'A' just to alphabetize it before
 * the other polymorphic tests.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
class EmbeddedPolymorphismTests extends TestBase {

	/** */
	@Entity
	@Data
	@NoArgsConstructor
	private static class Handler {
		@Id
		private Long id;
		private Animal animal;

		public Handler(Animal animal) {
			this.animal = animal;
		}
	}

	@Index
	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	private static class Animal {
		private String name;
	}

	/** */
	@Subclass
	@Index
	@Data
	@EqualsAndHashCode(callSuper = true)
	@NoArgsConstructor
	private static class Mammal extends Animal {
		private boolean longHair;

		public Mammal(String name, boolean longHair) {
			super(name);
			this.longHair = longHair;
		}
	}

	/** */
	@Subclass
	@Index
	@Data
	@EqualsAndHashCode(callSuper = true)
	@NoArgsConstructor
	private static class Cat extends Mammal {
		private boolean hypoallergenic;

		public Cat(String name, boolean longHair, boolean hypoallergenic) {
			super(name, longHair);
			this.hypoallergenic = hypoallergenic;
		}
	}

	/** */
	@Subclass
	@Index
	@Data
	@EqualsAndHashCode(callSuper = true)
	@NoArgsConstructor
	private static class Dog extends Mammal {
		private int loudness;

		public Dog(String name, boolean longHair, int loudness) {
			super(name, longHair);
			this.loudness = loudness;
		}
	}

	/** */
	@Test
	void testRegistrationForwards() throws Exception {
		factory().register(Handler.class);

		factory().register(Mammal.class);
		factory().register(Cat.class);
		factory().register(Dog.class);
	}

	/** */
	@Test
	void testRegistrationBackwards() throws Exception {
		factory().register(Dog.class);
		factory().register(Cat.class);
		factory().register(Mammal.class);

		factory().register(Handler.class);
	}

	/** */
	@Test
	void embeddedBaseClassWorks() throws Exception {
		this.testRegistrationForwards();

		final Handler handler = new Handler(new Animal("Bob"));
		final Handler fetched = saveClearLoad(handler);

		assertThat(fetched.animal).isEqualTo(handler.animal);
	}

	/** */
	@Test
	void firstSubclassWorks() throws Exception {
		this.testRegistrationForwards();

		final Handler handler = new Handler(new Mammal("Bob", true));
		final Handler fetched = saveClearLoad(handler);

		assertThat(fetched.animal).isEqualTo(handler.animal);
	}

	/** */
	@Test
	void secondSubclassWorks() throws Exception {
		this.testRegistrationBackwards();

		final Handler handler = new Handler(new Cat("Bob", true, true));
		final Handler fetched = saveClearLoad(handler);

		assertThat(fetched.animal).isEqualTo(handler.animal);
	}

	/** */
	@Test
	void queryingOnIndexedPropertiesWorks() throws Exception {
		this.testRegistrationForwards();

		final Handler handler = new Handler(new Cat("Bob", true, true));
		ofy().save().entity(handler).now();
		ofy().clear();
		final Handler fetched = ofy().load().type(Handler.class).filter("animal.hypoallergenic", true).first().now();

		assertThat(fetched.animal).isEqualTo(handler.animal);
	}

	/** */
	@Entity
	@Data
	private static class BusyHandler {
		private @Id Long id;
		private List<Animal> animals = new ArrayList<>();
	}

	/** */
	@Test
	void collectionOfPolymorphismWorks() throws Exception {
		this.testRegistrationForwards();
		factory().register(BusyHandler.class);

		final BusyHandler handler = new BusyHandler();
		handler.animals.add(new Animal("Bob"));
		handler.animals.add(new Mammal("Bob", true));
		handler.animals.add(new Cat("Bob", true, true));

		final BusyHandler fetched = saveClearLoad(handler);

		assertThat(fetched.animals).isEqualTo(handler.animals);
	}

	/** */
	@Test
	void queryingOnCollectionWorks() throws Exception {
		this.testRegistrationForwards();
		factory().register(BusyHandler.class);

		final BusyHandler handler = new BusyHandler();
		handler.animals.add(new Animal("Bob"));
		handler.animals.add(new Mammal("Bob", true));
		ofy().save().entity(handler).now();

		final BusyHandler handler2 = new BusyHandler();
		handler2.animals.add(new Mammal("Bob", true));
		handler2.animals.add(new Cat("Bob", true, true));
		ofy().save().entity(handler2).now();


		final List<BusyHandler> both = ofy().load().type(BusyHandler.class).filter("animals.longHair", true).list();
		assertThat(both).containsExactly(handler, handler2);

		final List<BusyHandler> second = ofy().load().type(BusyHandler.class).filter("animals.hypoallergenic", true).list();
		assertThat(second).containsExactly(handler2);
	}

	/** */
	@Entity
	@Data
	@NoArgsConstructor
	private static class HandlerWithIndexedAnimal {
		@Id Long id;
		@Index Animal animal;

		public HandlerWithIndexedAnimal(Animal animal) {
			this.animal = animal;
		}
	}

	/** */
	@Test
	void indexedFirstSubclassWorks() throws Exception {
		factory().register(HandlerWithIndexedAnimal.class);
		factory().register(Mammal.class);

		final HandlerWithIndexedAnimal handler = new HandlerWithIndexedAnimal(new Mammal("Bob", true));
		final HandlerWithIndexedAnimal fetched = saveClearLoad(handler);

		assertThat(fetched.animal).isEqualTo(handler.animal);
	}

	/** */
	@Subclass(alsoLoad = "FakeDuck")
	@NoArgsConstructor
	private static class Platypus extends Animal {
	}

	/** */
	@Test
	void alsoLoadSubclassNames() throws Exception {
		factory().register(Handler.class);
		factory().register(Platypus.class);

		final FullEntity<?> animal = FullEntity.newBuilder().set("^d", StringValue.newBuilder("FakeDuck").setExcludeFromIndexes(true).build()).build();

		final FullEntity<?> handlerInitial = ofy().save().toEntity(new Handler());
		final FullEntity<?> handler = FullEntity.newBuilder(handlerInitial).set("animal", animal).build();

		final com.google.cloud.datastore.Key key = datastore().put(handler).getKey();

		final Handler fetched = (Handler)ofy().load().value(key).now();
		assertThat(fetched.animal).isInstanceOf(Platypus.class);
	}
}
