package com.googlecode.objectify.test;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.testng.annotations.Test;

/**
 * Basic tests for registering entities
 * 
 * @author Matt Quail http://madbean.com/
 */
public class RegisterTests extends TestBase
{
	@Entity
	public static class NonPublicConstructor
	{
		@Id
		Long id;

		private NonPublicConstructor() { }
	}

	@Entity
	public static class NoNoargConstructors
	{
		@Id
		Long id;

		public NoNoargConstructors(long id)
		{
			this.id = id;
		}
	}

	@Test
	public void testNoArgConstructor()
	{
		assertRegisterFails(NonPublicConstructor.class, IllegalStateException.class);
		assertRegisterFails(NoNoargConstructors.class, IllegalStateException.class);
	}

	private void assertRegisterFails(Class<?> entity, Class<? extends Exception> expectedException)
	{
		try
		{
			this.fact.register(entity);
			assert false : "Shouldn't be register " + entity.getName();
		}
		catch (Exception e)
		{
			assert expectedException.isInstance(e) : "Unexpected exception of type " + e.getClass();
		}
	}
}
