/*
 */

package com.googlecode.objectify.test;

import com.google.cloud.datastore.FullEntity;
import com.google.cloud.datastore.StringValue;
import com.google.cloud.datastore.Value;
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
		final FullEntity<?> ent = makeEntity(HasAlsoLoads.class)
				.set("oldStuff", "oldStuff")
				.build();
		final com.google.cloud.datastore.Key gkey = datastore().put(ent).getKey();

		final Key<HasAlsoLoads> key = Key.create(gkey);
		final HasAlsoLoads fetched = ofy().load().key(key).now();

		assertThat(fetched.getStuff()).isEqualTo("oldStuff");
		assertThat(fetched.getOtherStuff()).isNull();
	}

	/** */
	@Test
	void alsoLoadThrowsExceptionIfDuplicateKey() throws Exception {
		final FullEntity<?> ent = makeEntity(HasAlsoLoads.class)
				.set("stuff", "stuff")
				.set("oldStuff", "oldStuff")
				.build();
		final com.google.cloud.datastore.Key gkey = datastore().put(ent).getKey();

		assertThrows(Exception.class, () -> {
			final Key<HasAlsoLoads> key = Key.create(gkey);
			ofy().load().key(key).now();
		}, "Shouldn't be able to read data duplicated with @AlsoLoad");
	}

	/** */
	@Test
	void alsoLoadOnMethodWorks() throws Exception {
		final FullEntity<?> ent = makeEntity(HasAlsoLoads.class)
				.set("weirdStuff", "5")
				.build();
		final com.google.cloud.datastore.Key gkey = datastore().put(ent).getKey();

		final Key<HasAlsoLoads> key = Key.create(gkey);
		final HasAlsoLoads fetched = ofy().load().key(key).now();

		assertThat(fetched.getWeird()).isEqualTo(5);
	}

	/** */
	@Test
	void alsoLoadWorksWithinEmbeddedObjects() throws Exception {
		final FullEntity<?> ent = makeEntity(HasEmbedded.class)
			.set("fieldUser", makeEmbeddedEntityWithProperty("oldFoo", StringValue.of(TEST_VALUE)))
			.set("methodUser", makeEmbeddedEntityWithProperty("oldFoo", StringValue.of(TEST_VALUE)))
			.build();

		final com.google.cloud.datastore.Key gkey = datastore().put(ent).getKey();

		final Key<HasEmbedded> key = Key.create(gkey);
		final HasEmbedded fetched = ofy().load().key(key).now();

		assertThat(fetched.fieldUser.foo).isEqualTo(TEST_VALUE);
		assertThat(fetched.methodUser.foo).isEqualTo(TEST_VALUE);
	}

	/** */
	@Test
	void alsoLoadWorksWithinAlsoLoadEmbeddedObjects() throws Exception {
		final FullEntity<?> ent = makeEntity(HasEmbedded.class)
				.set("oldFieldUser", makeEmbeddedEntityWithProperty("oldFoo", StringValue.of(TEST_VALUE)))
				.set("oldMethodUser", makeEmbeddedEntityWithProperty("oldFoo", StringValue.of(TEST_VALUE)))
				.build();

		final com.google.cloud.datastore.Key gkey = datastore().put(ent).getKey();

		final Key<HasEmbedded> key = Key.create(gkey);
		final HasEmbedded fetched = ofy().load().key(key).now();

		assertThat(fetched.fieldUser.foo).isEqualTo(TEST_VALUE);
		assertThat(fetched.methodUser.foo).isEqualTo(TEST_VALUE);
	}

	/** */
	@Test
	void alsoLoadWorksWithinEmbeddedArrays() throws Exception {
		final List<Value<FullEntity<?>>> list = Arrays.asList(
			makeEmbeddedEntityWithProperty("oldFoo", StringValue.of(TEST_VALUE)),
			makeEmbeddedEntityWithProperty("oldFoo", StringValue.of(TEST_VALUE))
		);

		final FullEntity<?> ent = makeEntity(HasEmbeddedArray.class)
				.set("fieldUsers", list)
				.set("methodUsers", list)
				.build();

		final com.google.cloud.datastore.Key gkey = datastore().put(ent).getKey();

		final Key<HasEmbeddedArray> key = Key.create(gkey);
		final HasEmbeddedArray fetched = ofy().load().key(key).now();

		assertThat(fetched.fieldUsers).asList()
				.containsExactly(new HasAlsoLoadField(TEST_VALUE), new HasAlsoLoadField(TEST_VALUE));

		assertThat(fetched.methodUsers).asList()
				.containsExactly(new HasAlsoLoadMethod(TEST_VALUE), new HasAlsoLoadMethod(TEST_VALUE));
	}

	/** */
	@Test
	void alsoLoadWorksWithinAlsoLoadEmbeddedArrays() throws Exception {
		final List<Value<FullEntity<?>>> list = Arrays.asList(
				makeEmbeddedEntityWithProperty("oldFoo", StringValue.of(TEST_VALUE)),
				makeEmbeddedEntityWithProperty("oldFoo", StringValue.of(TEST_VALUE))
		);

		final FullEntity<?> ent = makeEntity(HasEmbeddedArray.class)
				.set("oldFieldUsers", list)
				.set("oldMethodUsers", list)
				.build();

		final com.google.cloud.datastore.Key gkey = datastore().put(ent).getKey();

		final Key<HasEmbeddedArray> key = Key.create(gkey);
		final HasEmbeddedArray fetched = ofy().load().key(key).now();

		assertThat(fetched.fieldUsers).asList()
				.containsExactly(new HasAlsoLoadField(TEST_VALUE), new HasAlsoLoadField(TEST_VALUE));

		assertThat(fetched.methodUsers).asList()
				.containsExactly(new HasAlsoLoadMethod(TEST_VALUE), new HasAlsoLoadMethod(TEST_VALUE));
	}
}