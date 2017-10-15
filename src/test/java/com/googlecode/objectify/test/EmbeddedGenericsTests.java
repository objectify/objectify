/*
 */

package com.googlecode.objectify.test;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.test.util.TestBase;
import lombok.Data;

import java.io.Serializable;

/**
 * Tests of embedding generic classes
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
class EmbeddedGenericsTests extends TestBase {

	@Entity
	private static class HasGeneric {
		@Id
		private Long id;
		private IsGeneric<Integer> genericInt;
	}

	/** T needs to extend an interface */
	@Data
	private static class IsGeneric<T extends Serializable> implements Serializable {
		private T value;
	}

	/**
	 * This doesn't work for effectively the same reason that generic entity classes
	 * do not work. Java doesn't help us track the type parameter; Objectify would
	 * have to do that itself, effectively reifying generics. It's not inconceivable
	 * in the future but it feels like madness now.
	 */
//	@Test
//	void registersEntityWithGenericField() throws Exception {
//		factory().register(HasGeneric.class);
//	}


//	@Test
//	void temp() throws Exception {
//		Field f = HasGeneric.class.getField("genericInt");
//		Type t = f.getGenericType();
//		System.out.println(t.toString());
//	}

}