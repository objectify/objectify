/*
 */

package com.googlecode.objectify.test;

import java.util.List;
import java.util.logging.Logger;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.testng.annotations.Test;

import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.annotation.Subclass;

/**
 * Checking to make sure polymorphism works with generic base classes.
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class PolymorphicGenericClassTests extends TestBase
{
	/** */
	@SuppressWarnings("unused")
	private static Logger log = Logger.getLogger(PolymorphicGenericClassTests.class.getName());

	/** */
	@Entity
	public static class Vehicle<T>
	{
		@Id Long id;
		T name;
	}
	
	/** */
	@Subclass
	public static class Car extends Vehicle<String>
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
		List<Vehicle> all = ofy.query(Vehicle.class).list();
		assert all.size() == 1;
		assert all.get(0).name.equals(car.name);
	}
}
