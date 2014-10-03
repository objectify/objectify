/*
 */

package com.googlecode.objectify.test;

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

	@Index
	public static class LevelTwoIndexedClass {
		String bar="A";
	}
	public static class LevelTwoIndexedField {
		@Index String bar="A";
	}

	public static class LevelOne {
		String foo = "1";
		LevelTwoIndexedClass twoClass = new LevelTwoIndexedClass();
		LevelTwoIndexedField twoField = new LevelTwoIndexedField();
	}

	@Entity @Unindex
	public static class EntityWithEmbedded {
		@Id Long id;
		LevelOne one = new LevelOne();
		String prop = "A";
	}

	@SuppressWarnings("unused")
	public static class DefaultIndexedEmbed
	{
		@Index private boolean indexed = true;
		@Unindex private boolean unindexed = true;
		private boolean def = true;
	}


	@Entity
	public static class EmbeddedIndexedPojo
	{
		@Id Long id;

		@Unindex 	private boolean aProp = true;

		@Index 		private DefaultIndexedEmbed[] indexed = {new DefaultIndexedEmbed()};
		@Unindex 	private DefaultIndexedEmbed[] unindexed = {new DefaultIndexedEmbed()};
					@SuppressWarnings("unused")
					private DefaultIndexedEmbed[] def = {new DefaultIndexedEmbed()};

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
	public void setUpExtra()
	{
		fact().register(EmbeddedIndexedPojo.class);
		fact().register(EntityWithEmbedded.class);
	}

	/** */
	@Test
	public void testEmbeddedIndexedPojo() throws Exception
	{
		ofy().save().entity(new EmbeddedIndexedPojo()).now();

		assert  ofy().load().type(EmbeddedIndexedPojo.class).filter("indexed.indexed =", true).iterator().hasNext();
		assert  ofy().load().type(EmbeddedIndexedPojo.class).filter("indexed.def =", true).iterator().hasNext();
		assert !ofy().load().type(EmbeddedIndexedPojo.class).filter("indexed.unindexed=", true).iterator().hasNext();
		assert  ofy().load().type(EmbeddedIndexedPojo.class).filter("def.indexed =", true).iterator().hasNext();
		assert !ofy().load().type(EmbeddedIndexedPojo.class).filter("def.unindexed =", true).iterator().hasNext();
		assert !ofy().load().type(EmbeddedIndexedPojo.class).filter("def.def =", true).iterator().hasNext();
		assert !ofy().load().type(EmbeddedIndexedPojo.class).filter("unindexed.unindexed =", true).iterator().hasNext();
		assert  ofy().load().type(EmbeddedIndexedPojo.class).filter("unindexed.indexed =", true).iterator().hasNext();
		assert !ofy().load().type(EmbeddedIndexedPojo.class).filter("unindexed.def =", true).iterator().hasNext();

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
		ofy().save().entity(new EntityWithEmbedded()).now();

		assert !ofy().load().type(EntityWithEmbedded.class).filter("prop =", "A").iterator().hasNext();
		assert !ofy().load().type(EntityWithEmbedded.class).filter("one.foo =", "1").iterator().hasNext();
		assert  ofy().load().type(EntityWithEmbedded.class).filter("one.twoClass.bar =", "A").iterator().hasNext();
		assert  ofy().load().type(EntityWithEmbedded.class).filter("one.twoField.bar =", "A").iterator().hasNext();
	}
}