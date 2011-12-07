package com.googlecode.objectify.test;

import org.testng.annotations.Test;

import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.test.util.TestBase;
import com.googlecode.objectify.test.util.TestObjectify;
import com.googlecode.objectify.util.Monotonic;

/**
 */
public class MonotonicTests extends TestBase
{
	@Entity
	@Cache
	public static class HasNumber
	{
		@Id Long id;
		long number;
	}

	/** */
	@Test
	public void testMonotonic() throws Exception
	{
		this.fact.register(HasNumber.class);

		TestObjectify ofy = fact.begin();
		
		HasNumber hn = new HasNumber();
		hn.number = Monotonic.next(ofy, HasNumber.class, "number");
		assert hn.number == 1;
		
		ofy.put(hn);
		
		hn = new HasNumber();
		hn.number = Monotonic.next(ofy, HasNumber.class, "number");
		assert hn.number == 2;
	}
}
