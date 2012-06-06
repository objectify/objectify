/**
 *
 */
package com.googlecode.objectify.test;

import java.util.ArrayList;
import java.util.List;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.Result;
import com.googlecode.objectify.annotation.Embed;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.test.util.TestBase;

/**
 * @author Brian Chapman
 * 
 */
public class EmbeddedNullTests2 extends TestBase {

	@BeforeMethod
	public void setUp() {
		super.setUp();
		fact.register(FooBar.class);
	}

	@Test
	public void testFooBar() {
		Objectify ofy = fact.begin();
		
		FooBar fooBar = createFooBar();
		Result<Key<FooBar>> result = ofy.save().entity(fooBar);
		result.now();
		
		FooBar retreived = ofy.load().type(FooBar.class).id(fooBar.id).safeGet();

		assert fooBar.foos.size() == retreived.foos.size();
	}

	private FooBar createFooBar() {
		FooBar fooBar = new FooBar();
		List<Foo> foos = fooBar.foos;
		/* @formatter:off
		 * Here is the root of the issue. foos is an array where the first element is non-null but
		 * the remaining are null. In that case the underlying appengine Entity stores values like this
		 *
		 * fooBar.foos.bar.aField = [aField]
		 * fooBar.foos.bar.bField = [<someRandomNumber>]
		 * fooBar.foos.bar = [null, null, null, null]  <=== Note there are 4 here, not 5
		 *
		 * I would think that the expected behavior would be something like
		 *
		 * fooBar.foos.bar.aField = [aField, aField, aField, aField, aField]
		 * fooBar.foos.bar.bField = [1,2,3,4,5]
		 * 
		 * JMS:  this actually should produce a metaproperty .^null = [1,2,3,4]
		 */
		for (int i = 0; i < 5; i++) {
			Foo foo = createFoo();
			if (i != 0) {
				foo.bar = null;
			}
			foos.add(foo);
		}
		
		return fooBar;
	}

	private Foo createFoo() {
		Bar bar = new Bar();
		Foo foo = new Foo(bar);
		return foo;
	}

	@Embed
	public static class Bar {
		public String aField = "aField";
		public Double bField = Math.random();
	}

	@Embed
	public static class Foo {
		public Bar bar;
		public Foo(Bar bar) { this.bar = bar; }
		public Foo() { }
	}

	@Entity
	public static class FooBar {
		@Id Long id;
		public List<Foo> foos = new ArrayList<Foo>();
	}
}
