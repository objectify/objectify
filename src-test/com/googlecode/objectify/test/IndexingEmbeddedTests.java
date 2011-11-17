/*
 */

package com.googlecode.objectify.test;

import java.util.logging.Logger;

import javax.persistence.Embedded;
import javax.persistence.Id;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.Unindex;
import com.googlecode.objectify.test.IndexingInheritanceTests.IndexedDefaultPojo;
import com.googlecode.objectify.test.util.TestBase;
import com.googlecode.objectify.test.util.TestObjectify;

/**
 * Tests of @Indexed and @Unindexed
 * 
 * @author Scott Hernandez
 * @author Jeff Schnitzer
 */
public class IndexingEmbeddedTests extends TestBase
{
	/** */
	@SuppressWarnings("unused")
	private static Logger log = Logger.getLogger(IndexingEmbeddedTests.class.getName());

	@Index
	public static class LevelTwoIndexedClass 
	{
	   String bar="A";
	}
	public static class LevelTwoIndexedField 
	{
		@Index String bar="A"; 
	}

	public static class LevelOne {
	    String foo = "1";
	    @Embedded LevelTwoIndexedClass twoClass = new LevelTwoIndexedClass();
	    @Embedded LevelTwoIndexedField twoField = new LevelTwoIndexedField();
	}

	@Entity @Unindex 
 	public static class EntityWithEmbedded {
	    @Id Long id;
	    @Embedded LevelOne one = new LevelOne();
	    String prop = "A";
	}

	@SuppressWarnings("unused")
	public static class EmbeddedIndexedPojo
	{
		@Id Long id;

		@Unindex 				private boolean aProp = true;
		
		@Index 	@Embedded 	private IndexedDefaultPojo[] indexed = {new IndexedDefaultPojo()};
		@Unindex 	@Embedded 	private IndexedDefaultPojo[] unindexed = {new IndexedDefaultPojo()};
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

	/** */
	@BeforeMethod
	public void setUp()
	{
		super.setUp();
		
		this.fact.register(IndexedDefaultPojo.class);
		this.fact.register(EmbeddedIndexedPojo.class);
		this.fact.register(EntityWithEmbedded.class);
	}	
	
	/** */
	@Test
	public void testEmbeddedIndexedPojo() throws Exception
	{
		TestObjectify ofy = this.fact.begin();
		ofy.put(new EmbeddedIndexedPojo());

		assert  ofy.load().type(EmbeddedIndexedPojo.class).filter("indexed.indexed =", true).iterator().hasNext();
		assert  ofy.load().type(EmbeddedIndexedPojo.class).filter("indexed.def =", true).iterator().hasNext();
		assert !ofy.load().type(EmbeddedIndexedPojo.class).filter("indexed.unindexed=", true).iterator().hasNext();
		assert  ofy.load().type(EmbeddedIndexedPojo.class).filter("def.indexed =", true).iterator().hasNext();
		assert !ofy.load().type(EmbeddedIndexedPojo.class).filter("def.unindexed =", true).iterator().hasNext();
		assert  ofy.load().type(EmbeddedIndexedPojo.class).filter("def.def =", true).iterator().hasNext();
		assert !ofy.load().type(EmbeddedIndexedPojo.class).filter("unindexed.unindexed =", true).iterator().hasNext();
		assert  ofy.load().type(EmbeddedIndexedPojo.class).filter("unindexed.indexed =", true).iterator().hasNext();
		assert !ofy.load().type(EmbeddedIndexedPojo.class).filter("unindexed.def =", true).iterator().hasNext();
		
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
		TestObjectify ofy = this.fact.begin();
		ofy.put(new EntityWithEmbedded());
		
		assert !ofy.load().type(EntityWithEmbedded.class).filter("prop =", "A").iterator().hasNext();
		assert !ofy.load().type(EntityWithEmbedded.class).filter("one.foo =", "1").iterator().hasNext();
		assert  ofy.load().type(EntityWithEmbedded.class).filter("one.twoClass.bar =", "A").iterator().hasNext();
		assert  ofy.load().type(EntityWithEmbedded.class).filter("one.twoField.bar =", "A").iterator().hasNext();
	}	
}