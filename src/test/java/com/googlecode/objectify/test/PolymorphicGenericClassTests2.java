/*
 */

package com.googlecode.objectify.test;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Subclass;
import com.googlecode.objectify.test.util.TestBase;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static com.googlecode.objectify.ObjectifyService.factory;
import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * Checking to make sure polymorphism works with generic base classes - the
 * more complicated version.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
class PolymorphicGenericClassTests2 extends TestBase {

	@Data
	@SuppressWarnings("rawtypes")
	private static abstract class Ent<T extends Ent> {
		@Id Long id;

		@SuppressWarnings("unchecked")
		Key<T> key()
		{
			return Key.create((Class<? extends T>)this.getClass(), this.id);
		}
	}

	/** */
	@Entity
	@Data
	@EqualsAndHashCode(callSuper = true)
	@SuppressWarnings("rawtypes")
	private static class Vehicle<T extends Ent> extends Ent<T> {
		String name;
	}

	/** */
	@Subclass(index=true)
	@Data
	@EqualsAndHashCode(callSuper = true)
	private static class Car extends Vehicle<Car> {
		int numWheels;
	}

	/** */
	@Test
	void testQuery() throws Exception {
		factory().register(Vehicle.class);
		factory().register(Car.class);

		final Car car = new Car();
		car.name = "Fast";
		final Car c2 = saveClearLoad(car);
		assertThat(c2).isEqualTo(car);

		final List<Vehicle> all = ofy().load().type(Vehicle.class).list();
		assertThat(all).containsExactly(car);
	}
}
