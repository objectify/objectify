/*
 */

package com.googlecode.objectify.test;

import com.googlecode.objectify.test.entity.Trivial;
import com.googlecode.objectify.test.util.TestBase;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static com.googlecode.objectify.ObjectifyService.factory;
import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
class EntityConversionTests extends TestBase {

	/** */
	@Test
	void convertsToPojoAndBack() throws Exception {
		factory().register(Trivial.class);

		final Trivial triv = new Trivial(123L, "blah", 456);

		final com.google.appengine.api.datastore.Entity ent = ofy().save().toEntity(triv);
		assertThat(ent.getKey().getId()).isEqualTo(123L);
		assertThat(ent.getProperty("someString")).isEqualTo("blah");
		assertThat(ent.getProperty("someNumber")).isEqualTo(456L);

		final Trivial converted = ofy().load().fromEntity(ent);
		assertThat(converted).isEqualTo(triv);
	}

}