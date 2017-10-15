/*
 */

package com.googlecode.objectify.test;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.Unindex;
import com.googlecode.objectify.test.util.TestBase;
import lombok.Data;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static com.googlecode.objectify.ObjectifyService.factory;
import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * Tests of @Index and @Unindex
 *
 * @author Scott Hernandez
 * @author Jeff Schnitzer
 */
class IndexingEmbeddedTests extends TestBase {

	@Index
	@Data
	private static class LevelTwoIndexedClass {
		String bar="A";
	}

	@Data
	private static class LevelTwoIndexedField {
		@Index String bar="A";
	}

	@Data
	private static class LevelOne {
		String foo = "1";
		LevelTwoIndexedClass twoClass = new LevelTwoIndexedClass();
		LevelTwoIndexedField twoField = new LevelTwoIndexedField();
	}

	@Entity
	@Unindex
	@Data
	private static class EntityWithEmbedded {
		@Id Long id;
		LevelOne one = new LevelOne();
		String prop = "A";
	}

	@Data
	@SuppressWarnings("unused")
	private static class DefaultIndexedEmbed {
		@Index private boolean indexed = true;
		@Unindex private boolean unindexed = true;
		private boolean def = true;
	}

	@Entity
	@Data
	private static class EmbeddedIndexedPojo {
		@Id Long id;

		@Unindex 	private boolean aProp = true;

		@Index 		private DefaultIndexedEmbed[] indexed = {new DefaultIndexedEmbed()};
		@Unindex 	private DefaultIndexedEmbed[] unindexed = {new DefaultIndexedEmbed()};
					@SuppressWarnings("unused")
					private DefaultIndexedEmbed[] def = {new DefaultIndexedEmbed()};
	}

	/** */
	@BeforeEach
	void setUpExtra() {
		factory().register(EmbeddedIndexedPojo.class);
		factory().register(EntityWithEmbedded.class);
	}

	/** */
	@Test
	void testEmbeddedIndexedPojo() throws Exception {
		ofy().save().entity(new EmbeddedIndexedPojo()).now();

		assertThat(ofy().load().type(EmbeddedIndexedPojo.class).filter("indexed.indexed =", true)).isNotEmpty();
		assertThat(ofy().load().type(EmbeddedIndexedPojo.class).filter("indexed.def =", true)).isNotEmpty();
		assertThat(ofy().load().type(EmbeddedIndexedPojo.class).filter("indexed.unindexed=", true)).isEmpty();
		assertThat(ofy().load().type(EmbeddedIndexedPojo.class).filter("def.indexed =", true)).isNotEmpty();
		assertThat(ofy().load().type(EmbeddedIndexedPojo.class).filter("def.unindexed =", true)).isEmpty();
		assertThat(ofy().load().type(EmbeddedIndexedPojo.class).filter("def.def =", true)).isEmpty();
		assertThat(ofy().load().type(EmbeddedIndexedPojo.class).filter("unindexed.unindexed =", true)).isEmpty();
		assertThat(ofy().load().type(EmbeddedIndexedPojo.class).filter("unindexed.indexed =", true)).isNotEmpty();
		assertThat(ofy().load().type(EmbeddedIndexedPojo.class).filter("unindexed.def =", true)).isEmpty();
	}

	/** */
	@Test
	void testEmbeddedGraph() throws Exception {
		/*
		 * one.twoClass.bar = "A"
		 * one.twoField.bar = "A"
		 * one.foo = "1"
		 * id = ?
		 * prop = "A"
		 */
		ofy().save().entity(new EntityWithEmbedded()).now();

		assertThat(ofy().load().type(EntityWithEmbedded.class).filter("prop =", "A")).isEmpty();
		assertThat(ofy().load().type(EntityWithEmbedded.class).filter("one.foo =", "1")).isEmpty();
		assertThat(ofy().load().type(EntityWithEmbedded.class).filter("one.twoClass.bar =", "A")).isNotEmpty();
		assertThat(ofy().load().type(EntityWithEmbedded.class).filter("one.twoField.bar =", "A")).isNotEmpty();
	}
}