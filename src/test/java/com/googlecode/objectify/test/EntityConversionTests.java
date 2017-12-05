/*
 */

package com.googlecode.objectify.test;

import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.FullEntity;
import com.google.cloud.datastore.Key;
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

		final FullEntity<?> ent = ofy().save().toEntity(triv);
		final Key key = (Key)ent.getKey();
		assertThat(key.getId()).isEqualTo(123L);
		assertThat(ent.getString("someString")).isEqualTo("blah");
		assertThat(ent.getLong("someNumber")).isEqualTo(456L);

		final Entity completeEnt = Entity.newBuilder(key, ent).build();

		final Trivial converted = ofy().load().fromEntity(completeEnt);
		assertThat(converted).isEqualTo(triv);
	}

}