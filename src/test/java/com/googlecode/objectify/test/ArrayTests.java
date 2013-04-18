/*
 */

package com.googlecode.objectify.test;

import java.util.Arrays;
import java.util.logging.Logger;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.test.entity.HasArrays;
import com.googlecode.objectify.test.util.TestBase;

import static com.googlecode.objectify.test.util.TestObjectifyService.fact;
import static com.googlecode.objectify.test.util.TestObjectifyService.ofy;

/**
 * Tests of persisting arrays
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class ArrayTests extends TestBase
{
	/** */
	@SuppressWarnings("unused")
	private static Logger log = Logger.getLogger(ArrayTests.class.getName());

	/** */
	@BeforeMethod
	public void setUpArrayTests()
	{
		fact().register(HasArrays.class);
	}

	/** */
	@Test
	public void testStringArrays() throws Exception
	{
		HasArrays hasa = new HasArrays();
		hasa.strings = new String[] { "red", "green" };

		Key<HasArrays> k = ofy().save().entity(hasa).now();

		HasArrays fetched = ofy().load().key(k).get();

		assert Arrays.equals(fetched.strings, hasa.strings);
	}

	/** */
	@Test
	public void testIntArrays() throws Exception
	{
		HasArrays hasa = new HasArrays();
		hasa.ints = new int[] { 5, 6 };

		Key<HasArrays> k = ofy().save().entity(hasa).now();

		HasArrays fetched = ofy().load().key(k).get();

		assert Arrays.equals(fetched.ints, hasa.ints);
	}

	/** */
	@Test
	public void testIntegerArrays() throws Exception
	{
		HasArrays hasa = new HasArrays();
		hasa.integers = new Integer[] { 5, 6 };

		Key<HasArrays> k = ofy().save().entity(hasa).now();

		HasArrays fetched = ofy().load().key(k).get();

		assert Arrays.equals(fetched.integers, hasa.integers);
	}

	/** */
	@Test
	public void testLongArrays() throws Exception
	{
		HasArrays hasa = new HasArrays();
		hasa.longs = new long[] { 5, 6 };

		Key<HasArrays> k = ofy().save().entity(hasa).now();

		HasArrays fetched = ofy().load().key(k).get();

		assert Arrays.equals(fetched.longs, hasa.longs);
	}

}