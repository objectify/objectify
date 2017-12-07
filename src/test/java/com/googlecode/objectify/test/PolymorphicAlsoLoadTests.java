/*
 */

package com.googlecode.objectify.test;

import com.google.cloud.datastore.FullEntity;
import com.google.cloud.datastore.StringValue;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.Subclass;
import com.googlecode.objectify.test.util.TestBase;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static com.googlecode.objectify.ObjectifyService.factory;
import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * Testing the alsoLoad attribute of the @Subclass annotation
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
class PolymorphicAlsoLoadTests extends TestBase {
	/** */
	@Entity
	@Index
	@Data
	public static class Animal {
		@Id Long id;
	}

	/** */
	@Subclass(alsoLoad = "FakeDuck")
	@Index
	@Data
	@EqualsAndHashCode(callSuper = true)
	public static class Platypus extends Animal {
	}

	/** */
	@Test
	void alsoLoadOfSubclass() throws Exception {
		factory().register(Animal.class);
		factory().register(Platypus.class);

		final FullEntity<?> platyInitial = ofy().save().toEntity(new Platypus());
		final FullEntity<?> fakeDuck = FullEntity.newBuilder(platyInitial).set("^d", StringValue.newBuilder("FakeDuck").setExcludeFromIndexes(true).build()).build();

		final com.google.cloud.datastore.Key key = datastore().put(fakeDuck).getKey();

		final Animal fetched = (Animal)ofy().load().value(key).now();
		assertThat(fetched).isInstanceOf(Platypus.class);
	}

}
