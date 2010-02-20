/*
 * $Id: BeanMixin.java 1075 2009-05-07 06:41:19Z lhoriman $
 * $URL: https://subetha.googlecode.com/svn/branches/resin/rtest/src/org/subethamail/rtest/util/BeanMixin.java $
 */

package com.googlecode.objectify.test;

import javax.persistence.Embedded;
import javax.persistence.Id;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.annotation.Cached;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Indexed;
import com.googlecode.objectify.annotation.Unindexed;

/**
 * Tests of @Indexed and @Unindexed
 * 
 * @author Scott Hernandez
 * @author Jeff Schnitzer
 */
public class IndexingTests extends TestBase
{
	/** */
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(IndexingTests.class);

	@Indexed
	public static class LevelTwoIndexedClass 
	{
	   String bar="A";
	}
	public static class LevelTwoIndexedField 
	{
		@Indexed String bar="A"; 
	}

	public static class LevelOne {
	    String foo = "1";
	    @Embedded LevelTwoIndexedClass twoClass = new LevelTwoIndexedClass();
	    @Embedded LevelTwoIndexedField twoField = new LevelTwoIndexedField();
	}

	@Entity @Unindexed 
 	public static class EntityWithEmbedded {
	    @Id Long id;
	    @Embedded LevelOne one = new LevelOne();
	    String prop = "A";
	}

	@SuppressWarnings("unused")
	public static class EmbeddedIndexedPojo
	{
		@Id Long id;

		@Unindexed 				private boolean aProp = true;
		
		@Indexed 	@Embedded 	private IndexedDefaultPojo[] indexed = {new IndexedDefaultPojo()};
		@Unindexed 	@Embedded 	private IndexedDefaultPojo[] unindexed = {new IndexedDefaultPojo()};
					@Embedded 	private IndexedDefaultPojo[] def = {new IndexedDefaultPojo()};

// 		Fundamentally broken; how to test bad-hetro behavior?

//		@Indexed 	@Embedded 	private List indexedHetro = new ArrayList();
//		@Unindexed 	@Embedded 	private List unindexedHetro = new ArrayList();
//					@Embedded 	private List defHetro = new ArrayList();
//		public EmbeddedIndexedPojo(){
//			indexedHetro.add(new IndexedDefaultPojo());
//			indexedHetro.add(new IndexedPojo());
//			
//			unindexedHetro.addAll(indexedHetro);
//			defHetro.addAll(indexedHetro);
//		}
	}

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
	
	/** */
	@BeforeMethod
	public void setUp()
	{
		super.setUp();
		
		this.fact.register(IndexedDefaultPojo.class);
		this.fact.register(IndexedPojo.class);
		this.fact.register(UnindexedPojo.class);
		this.fact.register(EmbeddedIndexedPojo.class);
		this.fact.register(EntityWithEmbedded.class);
	}	
	
	/** */
	@Test
	public void testIndexedPojo() throws Exception
	{
		Objectify ofy = this.fact.begin();
		ofy.put(new IndexedPojo());

		assert ofy.query(IndexedPojo.class).filter("indexed =", true).fetch().iterator().hasNext();
		assert ofy.query(IndexedPojo.class).filter("def =", true).fetch().iterator().hasNext();
		assert !ofy.query(IndexedPojo.class).filter("unindexed =", true).fetch().iterator().hasNext();
	}
	/** */
	@Test
	public void testUnindexedPojo() throws Exception
	{
		Objectify ofy = this.fact.begin();
		ofy.put(new UnindexedPojo());

		assert ofy.query(UnindexedPojo.class).filter("indexed =", true).fetch().iterator().hasNext();
		assert !ofy.query(UnindexedPojo.class).filter("def =", true).fetch().iterator().hasNext();
		assert !ofy.query(UnindexedPojo.class).filter("unindexed =", true).fetch().iterator().hasNext();
		
	}
	/** */
	@Test
	public void testIndexedDefaultPojo() throws Exception
	{
		Objectify ofy = this.fact.begin();
		ofy.put(new IndexedDefaultPojo());

		assert ofy.query(IndexedDefaultPojo.class).filter("indexed =", true).fetch().iterator().hasNext();
		assert ofy.query(IndexedDefaultPojo.class).filter("def =", true).fetch().iterator().hasNext();
		assert !ofy.query(IndexedDefaultPojo.class).filter("unindexed =", true).fetch().iterator().hasNext();
		
	}
	/** */
	@Test
	public void testEmbeddedIndexedPojo() throws Exception
	{
		Objectify ofy = this.fact.begin();
		ofy.put(new EmbeddedIndexedPojo());

		assert  ofy.query(EmbeddedIndexedPojo.class).filter("indexed.indexed =", true).fetch().iterator().hasNext();
		assert  ofy.query(EmbeddedIndexedPojo.class).filter("indexed.def =", true).fetch().iterator().hasNext();
		assert !ofy.query(EmbeddedIndexedPojo.class).filter("indexed.unindexed=", true).fetch().iterator().hasNext();
		assert  ofy.query(EmbeddedIndexedPojo.class).filter("def.indexed =", true).fetch().iterator().hasNext();
		assert !ofy.query(EmbeddedIndexedPojo.class).filter("def.unindexed =", true).fetch().iterator().hasNext();
		assert  ofy.query(EmbeddedIndexedPojo.class).filter("def.def =", true).fetch().iterator().hasNext();
		assert !ofy.query(EmbeddedIndexedPojo.class).filter("unindexed.unindexed =", true).fetch().iterator().hasNext();
		assert  ofy.query(EmbeddedIndexedPojo.class).filter("unindexed.indexed =", true).fetch().iterator().hasNext();
		assert !ofy.query(EmbeddedIndexedPojo.class).filter("unindexed.def =", true).fetch().iterator().hasNext();
		
	}
	/** */
	@Test
	public void testEmbeddedGraph() throws Exception
	{
		/*
		 * one.twoClass.bar = "A"
		 * one.twoField.bar = "A"
		 * one.foo = "1"
		 * id = ?
		 * prop = "A"
		 */
		Objectify ofy = this.fact.begin();
		ofy.put(new EntityWithEmbedded());
		
		assert !ofy.query(EntityWithEmbedded.class).filter("prop =", "A").fetch().iterator().hasNext();
		assert !ofy.query(EntityWithEmbedded.class).filter("one.foo =", "1").fetch().iterator().hasNext();
		assert  ofy.query(EntityWithEmbedded.class).filter("one.twoClass.bar =", "A").fetch().iterator().hasNext();
		assert  ofy.query(EntityWithEmbedded.class).filter("one.twoField.bar =", "A").fetch().iterator().hasNext();
	}	

}