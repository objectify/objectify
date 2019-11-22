/*
 */

package com.googlecode.objectify.test;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Ignore;
import com.googlecode.objectify.annotation.Load;
import com.googlecode.objectify.annotation.OnSave;
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
 * Tests the fetching system for normal fields. This uses the session so we can experiment with
 * loading and re-loading with different entity groups.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
class LoadFieldRefTestsUsingSession extends TestBase {
	private Trivial t1;
	private Trivial t2;
	private Trivial tNone1;
	private Trivial tNone2;
	private Key<Trivial> k1;
	private Key<Trivial> k2;
	private Key<Trivial> kNone1;
	private Key<Trivial> kNone2;

	/** */
	@BeforeEach
	void createTwo() {
		factory().register(Trivial.class);

		t1 = new Trivial("foo", 11);
		k1 = ofy().save().entity(t1).now();

		t2 = new Trivial("bar", 22);
		k2 = ofy().save().entity(t2).now();

		tNone1 = new Trivial(123L, "fooNone", 33);
		tNone2 = new Trivial(456L, "barNone", 44);

		kNone1 = Key.create(tNone1);
		kNone2 = Key.create(tNone2);
	}

	/** */
	@Entity
	@Data
	private static class HasEntitiesWithGroups {
		static class Single {}
		static class Multi {}

		@Id Long id;
		@Load(Single.class) Ref<Trivial> single;
		@Load(Multi.class) List<Ref<Trivial>> multi = new ArrayList<>();

		@Ignore boolean onSaveFired = false;
		@OnSave void onSave() { onSaveFired = true; }
	}

	/** */
	@Test
	void loadGroupsSpecifiedOnCachedEntitiesStillLoad() throws Exception {
		factory().register(HasEntitiesWithGroups.class);

		final HasEntitiesWithGroups he = new HasEntitiesWithGroups();
		he.single = Ref.create(k1);
		he.multi.add(Ref.create(k1));
		he.multi.add(Ref.create(k2));
		HasEntitiesWithGroups fetched = saveClearLoad(he);

		final Key<HasEntitiesWithGroups> hekey = Key.create(he);

		assertThat(fetched.single.isLoaded()).isFalse();
		assertThat(fetched.multi.get(0).isLoaded()).isFalse();
		assertThat(fetched.multi.get(1).isLoaded()).isFalse();

		assertThat(fetched.single.equivalent(k1)).isTrue();
		assertThat(fetched.single.equivalent(fetched.multi.get(0))).isTrue();

		fetched = ofy().load().group(HasEntitiesWithGroups.Single.class).key(hekey).now();
		assertThat(fetched.single.isLoaded()).isTrue();
		assertThat(fetched.multi.get(0).isLoaded()).isTrue();
		assertThat(fetched.multi.get(1).isLoaded()).isFalse();

		assertThat(fetched.single.get()).isSameInstanceAs(fetched.multi.get(0).get());

		assertThat(fetched.single.equivalent(fetched.multi.get(0))).isTrue();
		assertThat(fetched.single.equivalent(k1)).isTrue();
		assertThat(fetched.single.get()).isEqualTo(t1);

		fetched = ofy().load().group(HasEntitiesWithGroups.Multi.class).key(hekey).now();
		assertThat(fetched.single.isLoaded()).isTrue();
		assertThat(fetched.multi.get(0).isLoaded()).isTrue();
		assertThat(fetched.multi.get(1).isLoaded()).isTrue();

		assertThat(fetched.single.get()).isSameInstanceAs(fetched.multi.get(0).get());

		assertThat(fetched.multi.get(0).get()).isEqualTo(t1);
		assertThat(fetched.multi.get(1).get()).isEqualTo(t2);

		fetched = ofy().load().group(HasEntitiesWithGroups.Single.class).group(HasEntitiesWithGroups.Multi.class).key(hekey).now();
		assertThat(fetched.single.isLoaded()).isTrue();
		assertThat(fetched.multi.get(0).isLoaded()).isTrue();
		assertThat(fetched.multi.get(1).isLoaded()).isTrue();

		assertThat(fetched.single.get()).isSameInstanceAs(fetched.multi.get(0).get());

		assertThat(fetched.multi.get(0).get()).isEqualTo(t1);
		assertThat(fetched.multi.get(1).get()).isEqualTo(t2);
	}

	/**
	 * Fragment of other test, just easier to debug.
	 */
	@Test
	void loadGroupsSpecifiedOnCachedEntitiesStillLoad_simplerCase() throws Exception {
		factory().register(HasEntitiesWithGroups.class);

		final HasEntitiesWithGroups he = new HasEntitiesWithGroups();
		he.single = Ref.create(k1);

		HasEntitiesWithGroups fetched = saveClearLoad(he);
		assertThat(fetched.single.isLoaded()).isFalse();

		fetched = ofy().load().group(HasEntitiesWithGroups.Single.class).entity(he).now();
		assertThat(fetched.single.isLoaded()).isTrue();

		// The code internally performs a save to a throwaway Entity, but we want to make sure that doesn't have
		// any public effects.
		assertThat(fetched.onSaveFired).isFalse();
	}
}