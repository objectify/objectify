/*
 * $Id: BeanMixin.java 1075 2009-05-07 06:41:19Z lhoriman $
 * $URL: https://subetha.googlecode.com/svn/branches/resin/rtest/src/org/subethamail/rtest/util/BeanMixin.java $
 */

package com.googlecode.objectify.test;

import java.util.logging.Logger;

import org.testng.annotations.Test;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.EntitySubclass;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.test.util.TestBase;

/**
 * Test that we can have abstract base classes in a polymorphic hierarchy.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class PolymorphicAbstractBaseTests extends TestBase
{
	/** */
	@SuppressWarnings("unused")
	private static Logger log = Logger.getLogger(PolymorphicAbstractBaseTests.class.getName());

	/** */
	@Entity
	abstract public static class Base {
		@Id Long id;
		String foo;
	}

	/** */
	@EntitySubclass(index=true)
	public static class Subclass extends Base {
		boolean bar;
	}

	/** */
	@Test
	public void registersForwards() throws Exception {
		this.fact.register(Base.class);
		this.fact.register(Subclass.class);
	}

	/** */
	@Test
	public void registersBackwards() throws Exception {
		this.fact.register(Subclass.class);
		this.fact.register(Base.class);
	}

	/** */
	@Test
	public void basicFetch() throws Exception {
		this.registersForwards();

		Subclass sub = new Subclass();
		sub.foo = "foo";
		sub.bar = true;

		Subclass fetched = this.putClearGet(sub);
		assert sub.foo.equals(fetched.foo);
		assert sub.bar == fetched.bar;
	}
}
