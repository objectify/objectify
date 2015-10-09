/*
 */

package com.googlecode.objectify.test;

import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.Unindex;
import com.googlecode.objectify.test.util.TestBase;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.logging.Logger;

import static com.googlecode.objectify.test.util.TestObjectifyService.fact;
import static com.googlecode.objectify.test.util.TestObjectifyService.ofy;

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
	@Entity
	@Cache
	//@Unindex default is unindex
	public static class UnindexedPojo
	{
		@Id Long id;
		@Index private boolean indexed = true;
		@Unindex private boolean unindexed = true;
		private boolean def = true;
	}

	@SuppressWarnings("unused")
	@Entity
	@Cache
	@Index
	public static class IndexedPojo
	{
		@Id Long id;
		@Index private boolean indexed = true;
		@Unindex private boolean unindexed = true;
		private boolean def = true;
	}

	@SuppressWarnings("unused")
	@Entity
	@Cache
	public static class DefaultIndexedPojo
	{
		@Id Long id;
		@Index private boolean indexed = true;
		@Unindex private boolean unindexed = true;
		private boolean def = true;
	}

	@SuppressWarnings("unused")
	@Entity
	@Cache
	public static class DefaultIndexedChildFromUnindexedPojo extends UnindexedPojo
	{
		@Index private boolean indexedChild = true;
		@Unindex private boolean unindexedChild = true;
		private boolean defChild = true;
	}

	@SuppressWarnings("unused")
	@Entity
	@Cache
	public static class DefaultIndexedGrandChildFromUnindexedPojo extends DefaultIndexedChildFromUnindexedPojo
	{
		@Index private boolean indexedGrandChild = true;
		@Unindex private boolean unindexedGrandChild = true;
		private boolean defGrandChild = true;
	}

	/** Switches the default from unindexed to indexed, but shouldn't have any effect on base */
	@Entity
	@Cache
	@Index
	public static class DerivedAndIndexed extends UnindexedPojo
	{
	}

	/** Switches the default from indexed to unindexed, but shouldn't have any effect on base */
	@Entity
	@Cache
	@Unindex
	public static class DerivedAndUnindexed extends IndexedPojo
	{
	}

	/** */
	@BeforeMethod
	public void setUpExtra()
	{
		fact().register(DefaultIndexedPojo.class);
		fact().register(IndexedPojo.class);
		fact().register(UnindexedPojo.class);
		fact().register(DefaultIndexedChildFromUnindexedPojo.class);
		fact().register(DefaultIndexedGrandChildFromUnindexedPojo.class);
		fact().register(DerivedAndIndexed.class);
		fact().register(DerivedAndUnindexed.class);
	}

	/** */
	@Test
	public void testIndexedPojo() throws Exception
	{
		ofy().save().entity(new IndexedPojo()).now();

		assert ofy().load().type(IndexedPojo.class).filter("indexed =", true).iterator().hasNext();
		assert ofy().load().type(IndexedPojo.class).filter("def =", true).iterator().hasNext();
		assert !ofy().load().type(IndexedPojo.class).filter("unindexed =", true).iterator().hasNext();
	}
	/** */
	@Test
	public void testUnindexedPojo() throws Exception
	{
		ofy().save().entity(new UnindexedPojo()).now();

		assert ofy().load().type(UnindexedPojo.class).filter("indexed =", true).iterator().hasNext();
		assert !ofy().load().type(UnindexedPojo.class).filter("def =", true).iterator().hasNext();
		assert !ofy().load().type(UnindexedPojo.class).filter("unindexed =", true).iterator().hasNext();
	}
	/** */
	@Test
	public void testDefaultIndexedPojo() throws Exception
	{
		ofy().save().entity(new DefaultIndexedPojo()).now();

		assert ofy().load().type(DefaultIndexedPojo.class).filter("indexed =", true).iterator().hasNext();
		assert !ofy().load().type(DefaultIndexedPojo.class).filter("def =", true).iterator().hasNext();
		assert !ofy().load().type(DefaultIndexedPojo.class).filter("unindexed =", true).iterator().hasNext();
	}

	@Test
	public void testDefaultIndexedChildFromUnindexedPojo() throws Exception
	{
		ofy().save().entity(new DefaultIndexedChildFromUnindexedPojo()).now();

		assert ofy().load().type(DefaultIndexedChildFromUnindexedPojo.class).filter("indexed =", true).iterator().hasNext();
		assert !ofy().load().type(DefaultIndexedChildFromUnindexedPojo.class).filter("def =", true).iterator().hasNext();
		assert !ofy().load().type(DefaultIndexedChildFromUnindexedPojo.class).filter("unindexed =", true).iterator().hasNext();

		assert ofy().load().type(DefaultIndexedChildFromUnindexedPojo.class).filter("indexedChild =", true).iterator().hasNext();
		assert !ofy().load().type(DefaultIndexedChildFromUnindexedPojo.class).filter("defChild =", true).iterator().hasNext();
		assert !ofy().load().type(DefaultIndexedChildFromUnindexedPojo.class).filter("unindexedChild =", true).iterator().hasNext();
	}

	@Test
	public void testDefaultIndexedGrandChildFromUnindexedPojo() throws Exception
	{
		ofy().save().entity(new DefaultIndexedGrandChildFromUnindexedPojo()).now();

		assert ofy().load().type(DefaultIndexedGrandChildFromUnindexedPojo.class).filter("indexed =", true).iterator().hasNext();
		assert !ofy().load().type(DefaultIndexedGrandChildFromUnindexedPojo.class).filter("def =", true).iterator().hasNext();
		assert !ofy().load().type(DefaultIndexedGrandChildFromUnindexedPojo.class).filter("unindexed =", true).iterator().hasNext();

		assert ofy().load().type(DefaultIndexedGrandChildFromUnindexedPojo.class).filter("indexedChild =", true).iterator().hasNext();
		assert !ofy().load().type(DefaultIndexedGrandChildFromUnindexedPojo.class).filter("defChild =", true).iterator().hasNext();
		assert !ofy().load().type(DefaultIndexedGrandChildFromUnindexedPojo.class).filter("unindexedChild =", true).iterator().hasNext();

		assert ofy().load().type(DefaultIndexedGrandChildFromUnindexedPojo.class).filter("indexedGrandChild =", true).iterator().hasNext();
		assert !ofy().load().type(DefaultIndexedGrandChildFromUnindexedPojo.class).filter("defGrandChild =", true).iterator().hasNext();
		assert !ofy().load().type(DefaultIndexedGrandChildFromUnindexedPojo.class).filter("unindexedGrandChild =", true).iterator().hasNext();
	}

	/** */
	@Test
	public void testDerivedAndIndexed() throws Exception
	{
		ofy().save().entity(new DerivedAndIndexed()).now();

		assert !ofy().load().type(DerivedAndIndexed.class).filter("def", true).iterator().hasNext();
	}

	/** */
	@Test
	public void testDerivedAndUnindexed() throws Exception
	{
		ofy().save().entity(new DerivedAndUnindexed()).now();

		assert ofy().load().type(DerivedAndUnindexed.class).filter("def", true).iterator().hasNext();
	}
}
