package com.googlecode.objectify.test;

import lombok.Data;
import org.junit.jupiter.api.Test;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.test.util.TestBase;

import static com.google.common.truth.Truth.assertThat;
import static com.googlecode.objectify.ObjectifyService.factory;
import static com.googlecode.objectify.ObjectifyService.ofy;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Basic tests for registering entities
 *
 * @author Matt Quail http://madbean.com/
 */
class RegisterTests extends TestBase {
	@Entity
	@Cache
	@Data
	private static class NonPublicConstructor {
		@Id Long id;
		private NonPublicConstructor() { }
	}

	@Entity
	@Cache
	@Data
	private static class NoNoargConstructors {
		@Id Long id;

		public NoNoargConstructors(long id) {
			this.id = id;
		}
	}

	@Data
	private static class BadStruct {
		int age;

		public BadStruct(int age) {
			this.age = age;
		}
	}

	@Entity
	@Cache
	@Data
	private static class HasEmbedded {
		@Id
		Long id;
		BadStruct name;
	}

	@Test
	void testNoArgConstructor() {
		assertRegisterSucceeds(NonPublicConstructor.class);

		// We can't actually check this on registration anymore, unfortunately.
//		assertRegisterFails(NoNoargConstructors.class, IllegalStateException.class);
//		assertRegisterFails(HasEmbedded.class, IllegalStateException.class);
	}

	private void assertRegisterSucceeds(Class<?> entity) {
		factory().register(entity);
	}

	private void assertRegisterFails(Class<?> entity, Class<? extends Exception> expectedException) {
		assertThrows(Exception.class, () -> {
			factory().register(entity);
		}, "Should be able to register" + entity.getName());
	}

	@Entity
	@Cache
	@Data
	private static class Normal {
		@Id Long id;
		String foo;
	}

	/**
	 * Checking https://code.google.com/p/objectify-appengine/issues/detail?id=146
	 */
	@Test
	void makeKeyWithoutRegstering() throws Exception {
		final Key<Normal> k = Key.create(Normal.class, 123L);

		// New behavior
		assertThat(ofy().load().key(k).now()).isNull();
	}
}
