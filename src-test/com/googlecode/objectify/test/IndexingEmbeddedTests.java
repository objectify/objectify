/*
 */

package com.googlecode.objectify.test;

import java.util.logging.Logger;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.googlecode.objectify.annotation.Embed;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.Unindex;
import com.googlecode.objectify.test.IndexingInheritanceTests.DefaultIndexedPojo;
import com.googlecode.objectify.test.util.TestBase;
import com.googlecode.objectify.test.util.TestObjectify;

/**
 * Tests of @Index and @Unindex
 * 
 * @author Scott Hernandez
 * @author Jeff Schnitzer
 */
public class IndexingEmbeddedTests extends TestBase
{
	/** */
	@SuppressWarnings("unused")
	private static Logger log = Logger.getLogger(IndexingEmbeddedTests.class.getName());

	@Entity
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
	    @Embed LevelTwoIndexedClass twoClass = new LevelTwoIndexedClass();
	    @Embed LevelTwoIndexedField twoField = new LevelTwoIndexedField();
	}

	@Entity @Unindex 
 	public static class EntityWithEmbedded {
	    @Id Long id;
	    @Embed LevelOne one = new LevelOne();
	    String prop = "A";
	}

	@Entity
	@SuppressWarnings("unused")
	public static class EmbeddedIndexedPojo
	{
		@Id Long id;

		@Unindex 			private boolean aProp = true;
		
		@Index 		@Embed 	private DefaultIndexedPojo[] indexed = {new DefaultIndexedPojo()};
		@Unindex 	@Embed 	private DefaultIndexedPojo[] unindexed = {new DefaultIndexedPojo()};
					@Embed 	private DefaultIndexedPojo[] def = {new DefaultIndexedPojo()};

// 		Fundamentally broken; how to test bad-hetro behavior?

//		@Indexed 	@Embed 	private List indexedHetro = new ArrayList();
//		@Unindexed 	@Embed 	private List unindexedHetro = new ArrayList();
//					@Embed 	private List defHetro = new ArrayList();
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
		
		this.fact.register(DefaultIndexedPojo.class);
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
		assert !ofy.load().type(EmbeddedIndexedPojo.class).filter("def.def =", true).iterator().hasNext();
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