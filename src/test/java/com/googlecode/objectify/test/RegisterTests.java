package com.googlecode.objectify.test;

import org.testng.annotations.Test;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Embed;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.test.util.TestBase;

import static com.googlecode.objectify.test.util.TestObjectifyService.fact;
import static com.googlecode.objectify.test.util.TestObjectifyService.ofy;

/**
 * Basic tests for registering entities
 *
 * @author Matt Quail http://madbean.com/
 */
public class RegisterTests extends TestBase {
	@Entity
	@Cache
	public static class NonPublicConstructor {
		@Id Long id;
		private NonPublicConstructor() { }
	}

	@Entity
	@Cache
	public static class NoNoargConstructors {
		@Id Long id;

		public NoNoargConstructors(long id) {
			this.id = id;
		}
	}

	public static class BadStruct {
		int age;

		public BadStruct(int age) {
			this.age = age;
		}
	}

	@Entity
	@Cache
	public static class HasEmbedded {
		@Id
		Long id;
		@Embed
		BadStruct name;

	}


	@Test
	public void testNoArgConstructor() {
		assertRegisterSucceeds(NonPublicConstructor.class);
		assertRegisterFails(NoNoargConstructors.class, IllegalStateException.class);
		assertRegisterFails(HasEmbedded.class, IllegalStateException.class);
	}

	private void assertRegisterSucceeds(Class<?> entity) {
		try {
			fact().register(entity);
		} catch (Exception e) {
			assert false : "Unexpected exception of type " + e.getClass();
		}
	}

	private void assertRegisterFails(Class<?> entity, Class<? extends Exception> expectedException) {
		try {
			fact().register(entity);
			assert false : "Shouldn't be register " + entity.getName();
		} catch (Exception e) {
			assert expectedException.isInstance(e) : "Unexpected exception of type " + e.getClass();
		}
	}

	@Entity
	@Cache
	public static class Normal {
		@Id Long id;
		String foo;
	}

	/**
	 * Checking https://code.google.com/p/objectify-appengine/issues/detail?id=146
	 */
	@Test
	public void makeKeyWithoutRegstering() throws Exception {
		Key<Normal> k = Key.create(Normal.class, 123L);

		// Old behavior
//		try {
//			ofy.load().key(k).get();
//			assert false;
//		} catch (IllegalArgumentException ex) {
//			// correct
//		}

		// New behavior
		assert ofy().load().key(k).get() == null;
	}
}
