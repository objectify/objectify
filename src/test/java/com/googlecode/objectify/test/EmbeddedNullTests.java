package com.googlecode.objectify.test;

import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.test.entity.Name;
import com.googlecode.objectify.test.util.TestBase;
import lombok.Data;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static com.google.common.truth.Truth.assertThat;
import static com.googlecode.objectify.ObjectifyService.factory;
import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * Tests specifically dealing with nulls in embedded fields and collections
 */
class EmbeddedNullTests extends TestBase {
	/**
	 * Add an entry to the database that should never come back from null queries.
	 */
	@BeforeEach
	void setUpExtra() {
		factory().register(Criminal.class);

		final Criminal avoid = new Criminal();
		avoid.aliases = new Name[] { new Name("Bob", "Dobbs") };
		avoid.moreAliases = Collections.singletonList(new Name("Bob", "Dobbs"));
		ofy().save().entity(avoid).now();
	}

	/**
	 * Rule: nulls come back as nulls
	 * Rule: filtering collections filters by contents, so looking for null fails
	 */
	@Test
	void cannotFilterByNullCollections() throws Exception {
		final Criminal crim = new Criminal();
		crim.aliases = null;
		crim.moreAliases = null;

		final Criminal fetched = saveClearLoad(crim);
		assertThat(fetched.aliases).isNull();
		assertThat(fetched.moreAliases).isNull();

		final Iterator<Criminal> queried = ofy().load().type(Criminal.class).filter("aliases", null).iterator();
		assertThat(queried.hasNext()).isFalse();

		final Iterator<Criminal> queried2 = ofy().load().type(Criminal.class).filter("moreAliases", null).iterator();
		assertThat(queried2.hasNext()).isFalse();
	}

	/**
	 */
	@Test
	void emptyCollectionsAreJustLikeNullCollections() throws Exception {
		final Criminal crim = new Criminal();
		crim.aliases = new Name[0];
		crim.moreAliases = new ArrayList<>();

		final Criminal fetched = saveClearLoad(crim);
		assertThat(fetched.aliases).isNull();
		assertThat(fetched.moreAliases).isNull();

		final Iterator<Criminal> queried = ofy().load().type(Criminal.class).filter("aliases", null).iterator();
		assertThat(queried.hasNext()).isFalse();

		final Iterator<Criminal> queried2 = ofy().load().type(Criminal.class).filter("moreAliases", null).iterator();
		assertThat(queried2.hasNext()).isFalse();
	}

	/**
	 */
	@Test
	void collectionsCanContainNull() throws Exception {
		final Criminal crim = new Criminal();
		crim.aliases = new Name[] { null };
		crim.moreAliases = Arrays.asList(crim.aliases);

		final Criminal fetched = saveClearLoad(crim);
		assertThat(fetched.aliases).isEqualTo(crim.aliases);
		assertThat(fetched.moreAliases).isEqualTo(crim.moreAliases);
	}

	/**
	 */
	@Test
	void collectionContainingNullAndOtherStuff() throws Exception {
		final Criminal crim = new Criminal();
		crim.aliases = new Name[] { new Name("Bob", "Dobbs"), null, new Name("Ivan", "Stang") };
		crim.moreAliases = Arrays.asList(crim.aliases);

		final Criminal fetched = saveClearLoad(crim);

		assertThat(fetched.aliases).isEqualTo(crim.aliases);
		assertThat(fetched.moreAliases).isEqualTo(crim.moreAliases);
	}

	/**
	 * Reported error when a field is null in an embedded set, but it seems to work
	 */
	@Test
	void testEmbeddedSetWithNullField() throws Exception {
		final Criminal crim = new Criminal();
		crim.aliases = new Name[] { new Name("Bob", "Dobbs"), new Name("Mojo", null), new Name("Ivan", "Stang") };
		crim.aliasesSet = new HashSet<>(Arrays.asList(crim.aliases));

		final Criminal fetched = saveClearLoad(crim);

		assertThat(fetched.aliasesSet).isEqualTo(crim.aliasesSet);
	}

	/**
	 * Entity for testing null/empty embedded arrays and collections
	 */
	@Entity
	@Cache
	@Data
	private static class Criminal {
		@Id
		Long id;

		Name[] aliases;

		List<Name> moreAliases;

		Set<Name> aliasesSet;
	}
}
