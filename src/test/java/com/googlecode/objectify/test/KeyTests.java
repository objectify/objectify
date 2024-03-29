/*
 */

package com.googlecode.objectify.test;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.test.entity.Trivial;
import com.googlecode.objectify.test.util.TestBase;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static com.googlecode.objectify.ObjectifyService.factory;
import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * Tests of Key behavior
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
class KeyTests extends TestBase {
	private static class NoEntity {
	}

	/** */
	@Test
	void kindOfClassWithoutEntityAnnotationIsClassSimpleName() throws Exception {
		final Key<NoEntity> key = Key.create(NoEntity.class, 123L);
		assertThat(key.getKind()).isEqualTo(NoEntity.class.getSimpleName());
	}

	/** */
	@Test
	void canRoundtripBothKindsOfUrlSafeStrings() throws Exception {
		final Key<Trivial> key = Key.create(Trivial.class, 123L);

		final String urlSafe = key.toUrlSafe();
		final Key<Trivial> urlSafeKey = Key.create(urlSafe);
		assertThat(urlSafeKey).isEqualTo(key);

		final String legacyUrlSafe = key.toLegacyUrlSafe();
		final Key<Trivial> legacyUrlSafeKey = Key.create(legacyUrlSafe);
		assertThat(legacyUrlSafeKey).isEqualTo(key);

		assertThat(urlSafe).isNotEqualTo(legacyUrlSafe);
	}

	/** */
	@Test
	void canRoundtripBothKindsOfUrlSafeStringsWithNamespace() throws Exception {
		final Key<Trivial> key = Key.create("namespace", Trivial.class, 123L);

		final String urlSafe = key.toUrlSafe();
		final Key<Trivial> urlSafeKey = Key.create(urlSafe);
		assertThat(urlSafeKey).isEqualTo(key);

		final String legacyUrlSafe = key.toLegacyUrlSafe();
		final Key<Trivial> legacyUrlSafeKey = Key.create(legacyUrlSafe);
		assertThat(legacyUrlSafeKey).isEqualTo(key);

		assertThat(urlSafe).isNotEqualTo(legacyUrlSafe);
	}

	@Test
	void urlsafeWorksForSavingAndLoading() throws Exception {
		factory().register(Trivial.class);

		final Trivial triv = new Trivial(123L, "foo", 5);
		final Key<Trivial> savedKey = ofy().save().entity(triv).now();

		final String urlSafe = savedKey.toUrlSafe();
		final Key<Trivial> reconstituted = Key.create(urlSafe);

		ofy().clear();
		final Trivial loaded = ofy().load().key(reconstituted).now();
		assertThat(loaded).isEqualTo(triv);
	}
}
