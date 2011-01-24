package com.googlecode.objectify.test;

import javax.persistence.Id;

import org.testng.annotations.Test;

import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.annotation.Cached;
import com.googlecode.objectify.util.Monotonic;

/**
 */
public class MonotonicTests extends TestBase
{
	@Cached
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

		Objectify ofy = fact.begin();
		
		HasNumber hn = new HasNumber();
		hn.number = Monotonic.next(ofy, HasNumber.class, "number");
		assert hn.number == 1;
		
		ofy.put(hn);
		
		hn = new HasNumber();
		hn.number = Monotonic.next(ofy, HasNumber.class, "number");
		assert hn.number == 2;
	}
}
