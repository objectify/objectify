/*
 */

package com.googlecode.objectify.test;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.test.util.TestBase;
import lombok.Data;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static com.googlecode.objectify.ObjectifyService.factory;
import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * Tests of basic query operations
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
class QueryCollectionTests extends TestBase {
	@Entity
	@Data
	private static class WithCollection {
		@Id
		Long id;

		@Index
		List<String> stuff = new ArrayList<>();
	}

	/** */
	@Test
	void queryForStringInList() throws Exception {
		factory().register(WithCollection.class);

		final WithCollection wc = new WithCollection();
		wc.stuff.add("foo");
		ofy().save().entity(wc).now();

		ofy().clear();
		final List<WithCollection> list = ofy().load().type(WithCollection.class).filter("stuff", "foo").list();

		assertThat(list).containsExactly(wc);
	}
}