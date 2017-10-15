/*
 */

package com.googlecode.objectify.test;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Load;
import com.googlecode.objectify.test.util.TestBase;
import lombok.Data;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static com.googlecode.objectify.ObjectifyService.factory;
import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * Tests of @Load(unless=Blah.class)
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
class LoadUnlessTests extends TestBase {
	static class Yes {}
	static class No {}

	/** */
	@Entity
	@Data
	private static class One {
		@Id long id;
		@Load(unless=No.class) Ref<Two> always;
		@Load(value=Yes.class, unless=No.class) Ref<Two> sometimes;
	}

	/** */
	@Entity
	@Data
	private static class Two {
		@Id long id;
	}

	/** */
	@Test
	void testLoadUnless() throws Exception {
		factory().register(One.class);
		factory().register(Two.class);

		final Two twoAlways = new Two();
		twoAlways.id = 456;
		final Key<Two> twoAlwaysKey = ofy().save().entity(twoAlways).now();
		final Ref<Two> twoAlwaysRef = Ref.create(twoAlwaysKey);

		final Two twoSometimes = new Two();
		twoSometimes.id = 789;
		final Key<Two> twoSometimesKey = ofy().save().entity(twoSometimes).now();
		final Ref<Two> twoSometimesRef = Ref.create(twoSometimesKey);

		final One one = new One();
		one.id = 123;
		one.always = twoAlwaysRef;
		one.sometimes = twoSometimesRef;
		ofy().save().entity(one).now();

		ofy().clear();
		final One fetchedDefault = ofy().load().entity(one).now();
		assertThat(fetchedDefault.always.isLoaded()).isTrue();
		assertThat(fetchedDefault.sometimes.isLoaded()).isFalse();

		ofy().clear();
		final One fetchedYes = ofy().load().group(Yes.class).entity(one).now();
		assertThat(fetchedYes.always.isLoaded()).isTrue();
		assertThat(fetchedYes.sometimes.isLoaded()).isTrue();

		ofy().clear();
		final One fetchedNo = ofy().load().group(No.class).entity(one).now();
		assertThat(fetchedNo.always.isLoaded()).isFalse();
		assertThat(fetchedNo.sometimes.isLoaded()).isFalse();

		ofy().clear();
		final One fetchedYesNo = ofy().load().group(Yes.class, No.class).entity(one).now();
		assertThat(fetchedYesNo.always.isLoaded()).isFalse();
		assertThat(fetchedYesNo.sometimes.isLoaded()).isFalse();
	}

	/** */
	@Entity
	@Data
	private static class Multi {
		@Id long id;
		@Load(unless=No.class) List<Ref<Two>> always = new ArrayList<>();
	}

	/** */
	@Test
	void testLoadUnlessMulti() throws Exception {
		factory().register(Multi.class);
		factory().register(Two.class);

		final Two two1 = new Two();
		two1.id = 456;
		final Key<Two> two1Key = ofy().save().entity(two1).now();
		final Ref<Two> two1Ref = Ref.create(two1Key);

		final Two two2 = new Two();
		two2.id = 789;
		final Key<Two> two2Key = ofy().save().entity(two2).now();
		final Ref<Two> two2Ref = Ref.create(two2Key);

		final Multi multi = new Multi();
		multi.id = 123;
		multi.always.add(two1Ref);
		multi.always.add(two2Ref);
		ofy().save().entity(multi).now();

		ofy().clear();
		final Multi fetchedDefault = ofy().load().entity(multi).now();
		assertThat(fetchedDefault.always).hasSize(2);
		for (final Ref<Two> ref : fetchedDefault.always) {
			assertThat(ref.isLoaded()).isEqualTo(true);
		}

		ofy().clear();
		final Multi fetchedNo = ofy().load().group(No.class).entity(multi).now();
		assertThat(fetchedNo.always).hasSize(2);
		for (final Ref<Two> ref : fetchedNo.always) {
			assertThat(ref.isLoaded()).isEqualTo(false);
		}
	}
}