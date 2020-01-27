/*
 */

package com.googlecode.objectify.test;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.test.entity.Trivial;
import com.googlecode.objectify.test.util.TestBase;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import static com.google.common.truth.Truth.assertThat;
import static com.googlecode.objectify.ObjectifyService.factory;
import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 */
class LotsOfKeysTests extends TestBase {

	/** */
	@Test
	void canFetchMoreThanAThousandThings() throws Exception {
		factory().register(Trivial.class);

		final List<Trivial> entities = LongStream.rangeClosed(1, 1001)
				.mapToObj(number -> new Trivial(number, "foo", number))
				.collect(Collectors.toList());

		final Set<Key<Trivial>> keys = ofy().save().entities(entities).now().keySet();
		assertThat(keys).hasSize(entities.size());

		final Collection<Trivial> fetched = ofy().load().keys(keys).values();
		assertThat(fetched).hasSize(entities.size());

		ofy().delete().keys(keys).now();

		final Collection<Trivial> shouldBeDeleted = ofy().load().keys(keys).values();
		assertThat(shouldBeDeleted).isEmpty();
	}
}