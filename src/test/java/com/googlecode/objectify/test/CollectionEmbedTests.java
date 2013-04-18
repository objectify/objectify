package com.googlecode.objectify.test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.testng.annotations.Test;

import com.googlecode.objectify.annotation.Embed;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.test.util.TestBase;

import static com.googlecode.objectify.test.util.TestObjectifyService.fact;

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
		@Override public String toString() { return this.getClass().getSimpleName() + "(" + value + ")"; }
	}

	@Test
	public void testHasSet() throws Exception
	{
		fact().register(HasSet.class);

		HasSet has = new HasSet();
		has.someSet.add(new HashableThing(4));
		has.someSet.add(new HashableThing(5));
		has.someSet.add(new HashableThing(6));

		HasSet fetched = this.putClearGet(has);

		assert fetched.someSet.size() == 3;
		assert fetched.someSet.contains(new HashableThing(4));
		assert fetched.someSet.contains(new HashableThing(5));
		assert fetched.someSet.contains(new HashableThing(6));
	}

	@Test
	public void testSetWithNull() throws Exception
	{
		fact().register(HasSet.class);

		HasSet has = new HasSet();
		has.someSet.add(null);

		HasSet fetched = this.putClearGet(has);

		assert fetched.someSet.size() == 1;
		assert fetched.someSet.contains(null);
	}

	/** Has an embed class in an embed collection */
	@Entity
	public static class HasDeepThings {
		@Id Long id;
		@Embed List<DeepThing> deeps = new ArrayList<DeepThing>();
		@Override public String toString() { return this.getClass().getSimpleName() + "(" + deeps + ")"; }
	}

	/** */
	public static class DeepThing {
		@Embed HashableThing thing;

		public DeepThing() {}
		public DeepThing(int val) { this.thing = new HashableThing(val); }
		@Override public String toString() { return this.getClass().getSimpleName() + "(" + thing + ")"; }
	}

	/** */
	@Test
	public void testHasDeepThings() throws Exception
	{
		fact().register(HasDeepThings.class);

		HasDeepThings has = new HasDeepThings();
		has.deeps.add(new DeepThing(4));
		has.deeps.add(new DeepThing(5));

		HasDeepThings fetched = this.putClearGet(has);

		assert fetched.deeps.size() == 2;
		assert fetched.deeps.get(0).thing.equals(has.deeps.get(0).thing);
		assert fetched.deeps.get(1).thing.equals(has.deeps.get(1).thing);
	}

}
