/*
 */

package com.googlecode.objectify.test;

import java.util.List;
import java.util.logging.Logger;

import com.googlecode.objectify.annotation.Entity;

import org.testng.annotations.Test;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.EntitySubclass;
import com.googlecode.objectify.test.util.TestBase;

/**
 * Checking to make sure polymorphism works with generic base classes - the
 * more complicated version.
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class PolymorphicGenericClassTests2 extends TestBase
{
	/** */
	@SuppressWarnings("unused")
	private static Logger log = Logger.getLogger(PolymorphicGenericClassTests2.class.getName());

	@SuppressWarnings("rawtypes")
	public static abstract class Ent<T extends Ent>
	{
		@Id public Long id;

		@SuppressWarnings("unchecked")
		public Key<T> key()
		{
			return Key.create((Class<? extends T>)this.getClass(), this.id);
		}
	}

	/** */
	@Entity
	@SuppressWarnings("rawtypes")
	public static class Vehicle<T extends Ent> extends Ent<T>
	{
		String name;
	}
	
	/** */
	@EntitySubclass(index=true)
	public static class Car extends Vehicle<Car>
	{
		int numWheels;
	}
	
	/** */
	@Test
	public void testQuery() throws Exception
	{
		this.fact.register(Vehicle.class);
		this.fact.register(Car.class);

		Car car = new Car();
		car.name = "Fast";
		Car c2 = this.putAndGet(car);
		assert car.name.equals(c2.name);

		Objectify ofy = this.fact.begin();
		
		@SuppressWarnings("rawtypes")
		List<Vehicle> all = ofy.load().type(Vehicle.class).list();
		assert all.size() == 1;
		assert all.get(0).name.equals(car.name);
	}
}
