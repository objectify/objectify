/*
 * $Id: BeanMixin.java 1075 2009-05-07 06:41:19Z lhoriman $
 * $URL: https://subetha.googlecode.com/svn/branches/resin/rtest/src/org/subethamail/rtest/util/BeanMixin.java $
 */

package com.googlecode.objectify.test;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.test.util.TestBase;
import org.testng.annotations.Test;

import java.util.logging.Logger;

import static com.googlecode.objectify.test.util.TestObjectifyService.fact;
import static com.googlecode.objectify.test.util.TestObjectifyService.ofy;

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
	@com.googlecode.objectify.annotation.Subclass(index=true)
	public static class Subclass extends Base {
		boolean bar;
	}

	/** */
	@Test
	public void registersForwards() throws Exception {
		fact().register(Base.class);
		fact().register(Subclass.class);
	}

	/** */
	@Test
	public void registersBackwards() throws Exception {
		fact().register(Subclass.class);
		fact().register(Base.class);
	}

	/** */
	@Test
	public void basicFetch() throws Exception {
		this.registersForwards();

		Subclass sub = new Subclass();
		sub.foo = "foo";
		sub.bar = true;

		Subclass fetched = ofy().putClearGet(sub);
		assert sub.foo.equals(fetched.foo);
		assert sub.bar == fetched.bar;
	}
}
