/*
 */

package com.googlecode.objectify.test;

import com.google.appengine.api.datastore.EmbeddedEntity;
import com.google.appengine.api.datastore.Entity;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.AlsoLoad;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Ignore;
import com.googlecode.objectify.test.util.TestBase;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static com.googlecode.objectify.ObjectifyService.factory;
import static com.googlecode.objectify.ObjectifyService.ofy;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests of using the @AlsoLoad annotation
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
class AlsoLoadTests extends TestBase {

	/** */
	private static final String TEST_VALUE = "blah";

	/** */
	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	private static class HasAlsoLoadField {
		@AlsoLoad("oldFoo")
		private String foo;
	}

	/** */
	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	private static class HasAlsoLoadMethod {
		private String foo;

		void set(@AlsoLoad("oldFoo") String oldFoo)
		{
			this.foo = oldFoo;
		}

	}

	/** */
	@com.googlecode.objectify.annotation.Entity
	@Cache
	private static class HasEmbedded {
		@Id Long id;
		@AlsoLoad("oldFieldUser") HasAlsoLoadField fieldUser;
		@AlsoLoad("oldMethodUser") HasAlsoLoadMethod methodUser;
	}

	/** */
	@com.googlecode.objectify.annotation.Entity
	@Cache
	private static class HasEmbeddedArray {
		@Id Long id;
		@AlsoLoad("oldFieldUsers") HasAlsoLoadField[] fieldUsers;
		@AlsoLoad("oldMethodUsers") HasAlsoLoadMethod[] methodUsers;
	}

	@com.googlecode.objectify.annotation.Entity
	@Cache
	@Data
	private static class HasAlsoLoads {
		@Id
		private Long id;

		@AlsoLoad("oldStuff")
		private String stuff;

		@AlsoLoad("oldOtherStuff")
		private String otherStuff;

		/** Tests loading with @AlsoLoad on a method */
		@Ignore
		private Integer weird;
		void namedAnything(final @AlsoLoad("weirdStuff") String stuff)
		{
			this.weird = Integer.valueOf(stuff);
		}
	}

	/**
	 * Add an entry to the database that should never come back from null queries.
	 */
	@BeforeEach
	void setUpExtra() {
		factory().register(HasAlsoLoads.class);
		factory().register(HasEmbedded.class);
		factory().register(HasEmbeddedArray.class);
	}

	/** */
	@Test
	void alsoLoadOnFieldWorks() throws Exception {
		final Entity ent = new Entity(Key.getKind(HasAlsoLoads.class));
		ent.setProperty("oldStuff", "oldStuff");
		ds().put(ent);

		final Key<HasAlsoLoads> key = Key.create(ent.getKey());
		final HasAlsoLoads fetched = ofy().load().key(key).now();

		assertThat(fetched.getStuff()).isEqualTo("oldStuff");
		assertThat(fetched.getOtherStuff()).isNull();
	}

	/** */
	@Test
	void alsoLoadThrowsExceptionIfDuplicateKey() throws Exception {
		final Entity ent = new Entity(Key.getKind(HasAlsoLoads.class));
		ent.setProperty("stuff", "stuff");
		ent.setProperty("oldStuff", "oldStuff");
		ds().put(ent);

		assertThrows(Exception.class, () -> {
			final Key<HasAlsoLoads> key = Key.create(ent.getKey());
			ofy().load().key(key).now();
		}, "Shouldn't be able to read data duplicated with @AlsoLoad");
	}

	/** */
	@Test
	void alsoLoadOnMethodWorks() throws Exception {
		final Entity ent = new Entity(Key.getKind(HasAlsoLoads.class));
		ent.setProperty("weirdStuff", "5");
		ds().put(ent);

		final Key<HasAlsoLoads> key = Key.create(ent.getKey());
		final HasAlsoLoads fetched = ofy().load().key(key).now();

		assertThat(fetched.getWeird()).isEqualTo(5);
	}

	/** */
	@Test
	void alsoLoadWorksWithinEmbeddedObjects() throws Exception {
		final Entity ent = new Entity(Key.getKind(HasEmbedded.class));
		ent.setProperty("fieldUser", makeEmbeddedEntityWithProperty("oldFoo", TEST_VALUE));
		ent.setProperty("methodUser", makeEmbeddedEntityWithProperty("oldFoo", TEST_VALUE));
		ds().put(ent);

		final Key<HasEmbedded> key = Key.create(ent.getKey());
		final HasEmbedded fetched = ofy().load().key(key).now();

		assertThat(fetched.fieldUser.foo).isEqualTo(TEST_VALUE);
		assertThat(fetched.methodUser.foo).isEqualTo(TEST_VALUE);
	}

	/** */
	@Test
	void alsoLoadWorksWithinAlsoLoadEmbeddedObjects() throws Exception {
		final Entity ent = new Entity(Key.getKind(HasEmbedded.class));
		ent.setProperty("oldFieldUser", makeEmbeddedEntityWithProperty("oldFoo", TEST_VALUE));
		ent.setProperty("oldMethodUser", makeEmbeddedEntityWithProperty("oldFoo", TEST_VALUE));
		ds().put(ent);

		final Key<HasEmbedded> key = Key.create(ent.getKey());
		final HasEmbedded fetched = ofy().load().key(key).now();

		assertThat(fetched.fieldUser.foo).isEqualTo(TEST_VALUE);
		assertThat(fetched.methodUser.foo).isEqualTo(TEST_VALUE);
	}

	/** */
	@Test
	void alsoLoadWorksWithinEmbeddedArrays() throws Exception {
		final Entity ent = new Entity(Key.getKind(HasEmbeddedArray.class));
		final List<EmbeddedEntity> list = Arrays.asList(
			makeEmbeddedEntityWithProperty("oldFoo", TEST_VALUE),
			makeEmbeddedEntityWithProperty("oldFoo", TEST_VALUE)
		);
		ent.setProperty("fieldUsers", list);
		ent.setProperty("methodUsers", list);
		ds().put(ent);

		final Key<HasEmbeddedArray> key = Key.create(ent.getKey());
		final HasEmbeddedArray fetched = ofy().load().key(key).now();

		assertThat(fetched.fieldUsers).asList()
				.containsExactly(new HasAlsoLoadField(TEST_VALUE), new HasAlsoLoadField(TEST_VALUE));

		assertThat(fetched.methodUsers).asList()
				.containsExactly(new HasAlsoLoadMethod(TEST_VALUE), new HasAlsoLoadMethod(TEST_VALUE));
	}

	/** */
	@Test
	void alsoLoadWorksWithinAlsoLoadEmbeddedArrays() throws Exception {
		final Entity ent = new Entity(Key.getKind(HasEmbeddedArray.class));
		final List<EmbeddedEntity> list = Arrays.asList(
				makeEmbeddedEntityWithProperty("oldFoo", TEST_VALUE),
				makeEmbeddedEntityWithProperty("oldFoo", TEST_VALUE)
		);
		ent.setProperty("oldFieldUsers", list);
		ent.setProperty("oldMethodUsers", list);
		ds().put(ent);

		final Key<HasEmbeddedArray> key = Key.create(ent.getKey());
		final HasEmbeddedArray fetched = ofy().load().key(key).now();

		assertThat(fetched.fieldUsers).asList()
				.containsExactly(new HasAlsoLoadField(TEST_VALUE), new HasAlsoLoadField(TEST_VALUE));

		assertThat(fetched.methodUsers).asList()
				.containsExactly(new HasAlsoLoadMethod(TEST_VALUE), new HasAlsoLoadMethod(TEST_VALUE));
	}
}