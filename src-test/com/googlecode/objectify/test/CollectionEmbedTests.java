package com.googlecode.objectify.test;

import java.util.HashSet;
import java.util.Set;

import org.testng.annotations.Test;

import com.googlecode.objectify.annotation.Embed;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.test.util.TestBase;

/**
 */
public class CollectionEmbedTests extends TestBase
{
	@Entity
	public static class HasSet
	{
		@Id Long id;
		@Embed Set<HashableThing> someSet = new HashSet<HashableThing>();
	}
	
	public static class HashableThing
	{
		Integer value;
		
		public HashableThing() {}
		public HashableThing(int val) { this.value = val; }
		
		@Override public int hashCode() { return value.hashCode(); }
		@Override public boolean equals(Object obj) { return value.equals(((HashableThing)obj).value); }
	}

	@Test
	public void testHasSet() throws Exception
	{
		this.fact.register(HasSet.class);
		
		HasSet has = new HasSet();
		has.someSet.add(new HashableThing(4));
		has.someSet.add(new HashableThing(5));
		has.someSet.add(new HashableThing(6));
		
		HasSet fetched = this.putAndGet(has);
		
		assert fetched.someSet.size() == 3;
		assert fetched.someSet.contains(new HashableThing(4));
		assert fetched.someSet.contains(new HashableThing(5));
		assert fetched.someSet.contains(new HashableThing(6));
	}

	@Test
	public void testSetWithNull() throws Exception
	{
		this.fact.register(HasSet.class);
		
		HasSet has = new HasSet();
		has.someSet.add(null);
		
		HasSet fetched = this.putAndGet(has);
		
		assert fetched.someSet.size() == 1;
		assert fetched.someSet.contains(null);
	}
}
