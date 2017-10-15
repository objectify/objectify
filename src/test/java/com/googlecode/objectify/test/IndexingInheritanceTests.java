/*
 */

package com.googlecode.objectify.test;

import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.Unindex;
import com.googlecode.objectify.test.util.TestBase;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static com.googlecode.objectify.ObjectifyService.factory;
import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * Tests of @Indexed and @Unindexed inherited by subclasses.
 *
 * @author Jeff Schnitzer
 */
class IndexingInheritanceTests extends TestBase {

	@SuppressWarnings("unused")
	@Entity
	@Cache
	@Data
	//@Unindex default is unindex
	private static class UnindexedPojo {
		@Id private Long id;
		@Index private boolean indexed = true;
		@Unindex private boolean unindexed = true;
		private boolean def = true;
	}

	@SuppressWarnings("unused")
	@Entity
	@Cache
	@Index
	@Data
	private static class IndexedPojo {
		@Id private Long id;
		@Index private boolean indexed = true;
		@Unindex private boolean unindexed = true;
		private boolean def = true;
	}

	@SuppressWarnings("unused")
	@Entity
	@Cache
	@Data
	private static class DefaultIndexedPojo {
		@Id private Long id;
		@Index private boolean indexed = true;
		@Unindex private boolean unindexed = true;
		private boolean def = true;
	}

	@SuppressWarnings("unused")
	@Entity
	@Cache
	@Data
	@EqualsAndHashCode(callSuper = true)
	private static class DefaultIndexedChildFromUnindexedPojo extends UnindexedPojo {
		@Index private boolean indexedChild = true;
		@Unindex private boolean unindexedChild = true;
		private boolean defChild = true;
	}

	@SuppressWarnings("unused")
	@Entity
	@Cache
	@Data
	@EqualsAndHashCode(callSuper = true)
	private static class DefaultIndexedGrandChildFromUnindexedPojo extends DefaultIndexedChildFromUnindexedPojo {
		@Index private boolean indexedGrandChild = true;
		@Unindex private boolean unindexedGrandChild = true;
		private boolean defGrandChild = true;
	}

	/** Switches the default from unindexed to indexed, but shouldn't have any effect on base */
	@Entity
	@Cache
	@Index
	@Data
	@EqualsAndHashCode(callSuper = true)
	private static class DerivedAndIndexed extends UnindexedPojo {
	}

	/** Switches the default from indexed to unindexed, but shouldn't have any effect on base */
	@Entity
	@Cache
	@Unindex
	@Data
	@EqualsAndHashCode(callSuper = true)
	private static class DerivedAndUnindexed extends IndexedPojo {
	}

	/** */
	@BeforeEach
	void setUpExtra() {
		factory().register(DefaultIndexedPojo.class);
		factory().register(IndexedPojo.class);
		factory().register(UnindexedPojo.class);
		factory().register(DefaultIndexedChildFromUnindexedPojo.class);
		factory().register(DefaultIndexedGrandChildFromUnindexedPojo.class);
		factory().register(DerivedAndIndexed.class);
		factory().register(DerivedAndUnindexed.class);
	}

	/** */
	@Test
	void testIndexedPojo() throws Exception {
		ofy().save().entity(new IndexedPojo()).now();

		assertThat(ofy().load().type(IndexedPojo.class).filter("indexed =", true)).isNotEmpty();
		assertThat(ofy().load().type(IndexedPojo.class).filter("def =", true)).isNotEmpty();
		assertThat(ofy().load().type(IndexedPojo.class).filter("unindexed =", true)).isEmpty();
	}

	/** */
	@Test
	void testUnindexedPojo() throws Exception {
		ofy().save().entity(new UnindexedPojo()).now();

		assertThat(ofy().load().type(UnindexedPojo.class).filter("indexed =", true)).isNotEmpty();
		assertThat(ofy().load().type(UnindexedPojo.class).filter("def =", true)).isEmpty();
		assertThat(ofy().load().type(UnindexedPojo.class).filter("unindexed =", true)).isEmpty();

	}
	/** */
	@Test
	void testDefaultIndexedPojo() throws Exception {
		ofy().save().entity(new DefaultIndexedPojo()).now();

		assertThat(ofy().load().type(DefaultIndexedPojo.class).filter("indexed =", true)).isNotEmpty();
		assertThat(ofy().load().type(DefaultIndexedPojo.class).filter("def =", true)).isEmpty();
		assertThat(ofy().load().type(DefaultIndexedPojo.class).filter("unindexed =", true)).isEmpty();
	}

	@Test
	void testDefaultIndexedChildFromUnindexedPojo() throws Exception {
		ofy().save().entity(new DefaultIndexedChildFromUnindexedPojo()).now();

		assertThat(ofy().load().type(DefaultIndexedChildFromUnindexedPojo.class).filter("indexed =", true)).isNotEmpty();
		assertThat(ofy().load().type(DefaultIndexedChildFromUnindexedPojo.class).filter("def =", true)).isEmpty();
		assertThat(ofy().load().type(DefaultIndexedChildFromUnindexedPojo.class).filter("unindexed =", true)).isEmpty();

		assertThat(ofy().load().type(DefaultIndexedChildFromUnindexedPojo.class).filter("indexedChild =", true)).isNotEmpty();
		assertThat(ofy().load().type(DefaultIndexedChildFromUnindexedPojo.class).filter("defChild =", true)).isEmpty();
		assertThat(ofy().load().type(DefaultIndexedChildFromUnindexedPojo.class).filter("unindexedChild =", true)).isEmpty();
	}

	@Test
	void testDefaultIndexedGrandChildFromUnindexedPojo() throws Exception {
		ofy().save().entity(new DefaultIndexedGrandChildFromUnindexedPojo()).now();

		assertThat(ofy().load().type(DefaultIndexedGrandChildFromUnindexedPojo.class).filter("indexed =", true)).isNotEmpty();
		assertThat(ofy().load().type(DefaultIndexedGrandChildFromUnindexedPojo.class).filter("def =", true)).isEmpty();
		assertThat(ofy().load().type(DefaultIndexedGrandChildFromUnindexedPojo.class).filter("unindexed =", true)).isEmpty();

		assertThat(ofy().load().type(DefaultIndexedGrandChildFromUnindexedPojo.class).filter("indexedChild =", true)).isNotEmpty();
		assertThat(ofy().load().type(DefaultIndexedGrandChildFromUnindexedPojo.class).filter("defChild =", true)).isEmpty();
		assertThat(ofy().load().type(DefaultIndexedGrandChildFromUnindexedPojo.class).filter("unindexedChild =", true)).isEmpty();

		assertThat(ofy().load().type(DefaultIndexedGrandChildFromUnindexedPojo.class).filter("indexedGrandChild =", true)).isNotEmpty();
		assertThat(ofy().load().type(DefaultIndexedGrandChildFromUnindexedPojo.class).filter("defGrandChild =", true)).isEmpty();
		assertThat(ofy().load().type(DefaultIndexedGrandChildFromUnindexedPojo.class).filter("unindexedGrandChild =", true)).isEmpty();
	}

	/** */
	@Test
	void testDerivedAndIndexed() throws Exception {
		ofy().save().entity(new DerivedAndIndexed()).now();

		assertThat(ofy().load().type(DerivedAndIndexed.class).filter("def", true)).isEmpty();
	}

	/** */
	@Test
	void testDerivedAndUnindexed() throws Exception {
		ofy().save().entity(new DerivedAndUnindexed()).now();

		assertThat(ofy().load().type(DerivedAndUnindexed.class).filter("def", true)).isNotEmpty();
	}
}