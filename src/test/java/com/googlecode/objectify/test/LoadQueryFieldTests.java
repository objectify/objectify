/*
 */

package com.googlecode.objectify.test;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Load;
import com.googlecode.objectify.test.entity.Trivial;
import com.googlecode.objectify.test.util.TestBase;
import lombok.Data;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static com.googlecode.objectify.ObjectifyService.factory;
import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * Tests the fetching via queries
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
class LoadQueryFieldTests extends TestBase {
	private Trivial t0;
	private Trivial t1;
	private Trivial tNone0;
	private Trivial tNone1;
	private Key<Trivial> k0;
	private Key<Trivial> k1;
	private Key<Trivial> kNone0;
	private Key<Trivial> kNone1;

	/** */
	@BeforeEach
	void createTwo() {
		factory().register(Trivial.class);

		t0 = new Trivial("foo", 11);
		k0 = ofy().save().entity(t0).now();

		t1 = new Trivial("bar", 22);
		k1 = ofy().save().entity(t1).now();

		tNone0 = new Trivial(123L, "fooNone", 33);
		tNone1 = new Trivial(456L, "barNone", 44);

		kNone0 = Key.create(tNone0);
		kNone1 = Key.create(tNone1);
	}

	/** */
	@Entity
	@Data
	private static class HasEntities {
		@Id Long id;
		@Load Ref<Trivial> single;
		@Load List<Ref<Trivial>> multi = new ArrayList<>();
	}

	/** */
	@Test
	void testTargetsExist() throws Exception {
		factory().register(HasEntities.class);

		final HasEntities he = new HasEntities();
		he.single = Ref.create(t0);
		he.multi.add(Ref.create(t0));
		he.multi.add(Ref.create(t1));

		final Key<HasEntities> hekey = ofy().save().entity(he).now();
		ofy().clear();

		final HasEntities fetched = ofy().load().type(HasEntities.class).filterKey("=", hekey).first().now();

		assertThat(fetched.single.get()).isEqualTo(t0);
		assertThat(fetched.multi.get(0).get()).isSameInstanceAs(fetched.single.get());
		assertThat(fetched.multi.get(1).get()).isEqualTo(t1);
	}
}