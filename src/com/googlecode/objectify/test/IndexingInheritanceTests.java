/*
 */

package com.googlecode.objectify.test;

import java.util.logging.Logger;

import javax.persistence.Id;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.annotation.Cached;
import com.googlecode.objectify.annotation.Indexed;
import com.googlecode.objectify.annotation.Unindexed;

/**
 * Tests of @Indexed and @Unindexed inherited by subclasses.
 * 
 * @author Jeff Schnitzer
 */
public class IndexingInheritanceTests extends TestBase
{
	/** */
	@SuppressWarnings("unused")
	private static Logger log = Logger.getLogger(IndexingInheritanceTests.class.getName());

	@SuppressWarnings("unused")
	@Cached
	@Unindexed
	public static class UnindexedPojo
	{
		@Id Long id;
		@Indexed private boolean indexed = true;
		@Unindexed private boolean unindexed = true;
		private boolean def = true;
	}
	
	@SuppressWarnings("unused")
	@Cached
	@Indexed
	public static class IndexedPojo
	{
		@Id Long id;
		@Indexed private boolean indexed = true;
		@Unindexed private boolean unindexed = true;
		private boolean def = true;
	}
	
	@SuppressWarnings("unused")
	@Cached
	public static class IndexedDefaultPojo
	{
		@Id Long id;
		@Indexed private boolean indexed = true;
		@Unindexed private boolean unindexed = true;
		private boolean def = true;
	}
	

	@SuppressWarnings("unused")
	@Cached
	public static class DefaultIndexedChildFromUnindexedPojo extends UnindexedPojo
	{
		@Indexed
		private boolean indexedChild = true;
		@Unindexed
		private boolean unindexedChild = true;
		private boolean defChild = true;
	}

	@SuppressWarnings("unused")
	@Cached
	public static class DefaultIndexedGrandChildFromUnindexedPojo extends DefaultIndexedChildFromUnindexedPojo
	{
		@Indexed
		private boolean indexedGrandChild = true;
		@Unindexed
		private boolean unindexedGrandChild = true;
		private boolean defGrandChild = true;
	}
	
	/** Switches the default from unindexed to indexed, but shouldn't have any effect on base */
	@Cached
	@Indexed
	public static class DerivedAndIndexed extends UnindexedPojo
	{
	}

	/** Switches the default from indexed to unindexed, but shouldn't have any effect on base */
	@Cached
	@Unindexed
	public static class DerivedAndUnindexed extends IndexedPojo
	{
	}
	
	/** */
	@BeforeMethod
	public void setUp()
	{
		super.setUp();
		
		this.fact.register(IndexedDefaultPojo.class);
		this.fact.register(IndexedPojo.class);
		this.fact.register(UnindexedPojo.class);
		this.fact.register(DefaultIndexedChildFromUnindexedPojo.class);
		this.fact.register(DefaultIndexedGrandChildFromUnindexedPojo.class);
		this.fact.register(DerivedAndIndexed.class);
		this.fact.register(DerivedAndUnindexed.class);
	}	
	
	/** */
	@Test
	public void testIndexedPojo() throws Exception
	{
		Objectify ofy = this.fact.begin();
		ofy.put(new IndexedPojo());

		assert ofy.query(IndexedPojo.class).filter("indexed =", true).iterator().hasNext();
		assert ofy.query(IndexedPojo.class).filter("def =", true).iterator().hasNext();
		assert !ofy.query(IndexedPojo.class).filter("unindexed =", true).iterator().hasNext();
	}
	/** */
	@Test
	public void testUnindexedPojo() throws Exception
	{
		Objectify ofy = this.fact.begin();
		ofy.put(new UnindexedPojo());

		assert ofy.query(UnindexedPojo.class).filter("indexed =", true).iterator().hasNext();
		assert !ofy.query(UnindexedPojo.class).filter("def =", true).iterator().hasNext();
		assert !ofy.query(UnindexedPojo.class).filter("unindexed =", true).iterator().hasNext();
		
	}
	/** */
	@Test
	public void testIndexedDefaultPojo() throws Exception
	{
		Objectify ofy = this.fact.begin();
		ofy.put(new IndexedDefaultPojo());

		assert ofy.query(IndexedDefaultPojo.class).filter("indexed =", true).iterator().hasNext();
		assert ofy.query(IndexedDefaultPojo.class).filter("def =", true).iterator().hasNext();
		assert !ofy.query(IndexedDefaultPojo.class).filter("unindexed =", true).iterator().hasNext();
		
	}

	@Test
	public void testDefaultIndexedChildFromUnindexedPojo() throws Exception
	{
		Objectify ofy = fact.begin();
		ofy.put(new DefaultIndexedChildFromUnindexedPojo());

		assert ofy.query(DefaultIndexedChildFromUnindexedPojo.class).filter("indexed =", true).iterator().hasNext();
		assert !ofy.query(DefaultIndexedChildFromUnindexedPojo.class).filter("def =", true).iterator().hasNext();
		assert !ofy.query(DefaultIndexedChildFromUnindexedPojo.class).filter("unindexed =", true).iterator().hasNext();

		assert ofy.query(DefaultIndexedChildFromUnindexedPojo.class).filter("indexedChild =", true).iterator().hasNext();
		assert ofy.query(DefaultIndexedChildFromUnindexedPojo.class).filter("defChild =", true).iterator().hasNext();
		assert !ofy.query(DefaultIndexedChildFromUnindexedPojo.class).filter("unindexedChild =", true).iterator().hasNext();
	}

	@Test
	public void testDefaultIndexedGrandChildFromUnindexedPojo() throws Exception
	{
		Objectify ofy = fact.begin();
		ofy.put(new DefaultIndexedGrandChildFromUnindexedPojo());

		assert ofy.query(DefaultIndexedGrandChildFromUnindexedPojo.class).filter("indexed =", true).iterator().hasNext();
		assert !ofy.query(DefaultIndexedGrandChildFromUnindexedPojo.class).filter("def =", true).iterator().hasNext();
		assert !ofy.query(DefaultIndexedGrandChildFromUnindexedPojo.class).filter("unindexed =", true).iterator().hasNext();

		assert ofy.query(DefaultIndexedGrandChildFromUnindexedPojo.class).filter("indexedChild =", true).iterator().hasNext();
		assert ofy.query(DefaultIndexedGrandChildFromUnindexedPojo.class).filter("defChild =", true).iterator().hasNext();
		assert !ofy.query(DefaultIndexedGrandChildFromUnindexedPojo.class).filter("unindexedChild =", true).iterator().hasNext();

		assert ofy.query(DefaultIndexedGrandChildFromUnindexedPojo.class).filter("indexedGrandChild =", true).iterator().hasNext();
		assert ofy.query(DefaultIndexedGrandChildFromUnindexedPojo.class).filter("defGrandChild =", true).iterator().hasNext();
		assert !ofy.query(DefaultIndexedGrandChildFromUnindexedPojo.class).filter("unindexedGrandChild =", true).iterator().hasNext();
	}
	
	/** */
	@Test
	public void testDerivedAndIndexed() throws Exception
	{
		Objectify ofy = fact.begin();
		ofy.put(new DerivedAndIndexed());
		
		assert !ofy.query(DerivedAndIndexed.class).filter("def", true).iterator().hasNext();
	}

	/** */
	@Test
	public void testDerivedAndUnindexed() throws Exception
	{
		Objectify ofy = fact.begin();
		ofy.put(new DerivedAndUnindexed());
		
		assert ofy.query(DerivedAndUnindexed.class).filter("def", true).iterator().hasNext();
	}
}