package com.googlecode.objectify.test;

import java.io.Serializable;
import java.util.Arrays;

import javax.persistence.Embedded;
import javax.persistence.Id;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.googlecode.objectify.annotation.Cached;
import com.googlecode.objectify.annotation.Serialized;
import com.googlecode.objectify.test.entity.Name;

/**
 * Tests of the {@code @Serialized} annotation
 */
public class SerializedTests extends TestBase
{
	public static class EmbeddedSerialized implements Serializable
	{
		private static final long serialVersionUID = 1L;

		@Serialized long[] longs;
	}
	
	@Cached
	public static class SerializedStuff
	{
		@Id public Long id;
		
		@Serialized public Name[] names;
		
		@Embedded public EmbeddedSerialized easy;
		
		@Embedded public EmbeddedSerialized[] hard;
	}

	/**
	 */
	@BeforeMethod
	public void setUp()
	{
		super.setUp();
		
		this.fact.register(SerializedStuff.class);
	}
	
	@Test
	public void testEverything() throws Exception
	{
		SerializedStuff ss = new SerializedStuff();
		ss.names = new Name[] { new Name("foo", "bar"), null };	// boring, will work just fine
		ss.easy = new EmbeddedSerialized();
		ss.easy.longs = new long[] { 1, 2, 3 };
		ss.hard = new EmbeddedSerialized[3];
		ss.hard[0] = new EmbeddedSerialized();
		ss.hard[0].longs = new long[] { 4, 5, 6 };
		ss.hard[1] = null;
		ss.hard[2] = new EmbeddedSerialized();
		ss.hard[2].longs = new long[] { 7, 8, 9 };
		
		SerializedStuff fetched = this.putAndGet(ss);
		
		assert Arrays.equals(fetched.names, ss.names);
		assert fetched.easy != null;
		assert Arrays.equals(fetched.easy.longs, ss.easy.longs);
		assert fetched.hard.length == ss.hard.length;
		assert Arrays.equals(fetched.hard[0].longs, ss.hard[0].longs);
		assert fetched.hard[1] == null;
		assert Arrays.equals(fetched.hard[2].longs, ss.hard[2].longs);
	}
}
