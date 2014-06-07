/*
 */

package com.googlecode.objectify.test;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.test.util.TestBase;

import java.io.Serializable;

/**
 * Tests of embedding generic classes
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class EmbeddedGenericsTests extends TestBase
{
	@Entity
	public static class HasGeneric {
		@Id Long id;
		public IsGeneric<Integer> genericInt;
	}

	/** T needs to extend an interface */
	public static class IsGeneric<T extends Serializable> implements Serializable {
		T value;
	}

	/**
	 * This doesn't work for effectively the same reason that generic entity classes
	 * do not work. Java doesn't help us track the type parameter; Objectify would
	 * have to do that itself, effectively reifying generics. It's not inconceivable
	 * in the future but it feels like madness now.
	 */
//	@Test
//	public void registersEntityWithGenericField() throws Exception {
//		fact().register(HasGeneric.class);
//	}


//	@Test
//	public void temp() throws Exception {
//		Field f = HasGeneric.class.getField("genericInt");
//		Type t = f.getGenericType();
//		System.out.println(t.toString());
//	}

}