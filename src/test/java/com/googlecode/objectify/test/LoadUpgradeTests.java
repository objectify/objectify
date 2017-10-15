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
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static com.googlecode.objectify.ObjectifyService.factory;
import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * Tests of reloading when there are load groups - basically upgrades.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
class LoadUpgradeTests extends TestBase {
	/** */
	@Entity
	@Data
	@NoArgsConstructor
	private static class Other {
		@Id long id;
		Other(long id) { this.id = id; }
	}

	private Key<Other> ko0;
	private Key<Other> ko1;
	private Other other0;
	private Other other1;

	/** */
	@BeforeEach
	void createTwoOthers() {
		factory().register(Other.class);

		other0 = new Other(123L);
		other1 = new Other(456L);

		ko0 = ofy().save().entity(other0).now();
		ko1 = ofy().save().entity(other1).now();
	}

	/** */
	@Entity
	@Data
	private static class HasMulti {
		static class Multi {}

		@Id Long id;
		@Load(Multi.class) List<Ref<Other>> multi = new ArrayList<>();
	}

	/** */
	@Test
	void withLoadGroupRefsAreLoaded() throws Exception {
		factory().register(HasMulti.class);

		final HasMulti hm = new HasMulti();
		hm.multi.add(Ref.create(ko0));
		hm.multi.add(Ref.create(ko1));

		final Key<HasMulti> hmkey = ofy().save().entity(hm).now();
		ofy().clear();
		final HasMulti fetched = ofy().load().group(HasMulti.Multi.class).key(hmkey).now();

		for (final Ref<Other> ref: fetched.multi)
			assertThat(ref.isLoaded()).isTrue();
	}

	/** */
	@Test
	void reloadingWithALoadGroupUpgradesMulti() throws Exception {
		factory().register(HasMulti.class);

		final HasMulti hm = new HasMulti();
		hm.multi.add(Ref.create(ko0));
		hm.multi.add(Ref.create(ko1));

		final HasMulti fetched = saveClearLoad(hm);

		for (final Ref<Other> ref: fetched.multi)
			assertThat(ref.isLoaded()).isFalse();

		final HasMulti reloaded = ofy().load().group(HasMulti.Multi.class).entity(hm).now();	// upgrade with multi

		for (final Ref<Other> ref: reloaded.multi)
			assertThat(ref.isLoaded()).isTrue();
	}

	/** */
	@Entity
	@Data
	private static class HasSingle {
		static class Single {}

		@Id Long id;
		@Load(Single.class) Ref<Other> single;
	}

	/** */
	@Test
	void reloadingWithALoadGroupUpgradesSingle() throws Exception {
		factory().register(HasSingle.class);

		final HasSingle hs = new HasSingle();
		hs.single = Ref.create(ko0);

		final HasSingle fetched = saveClearLoad(hs);
		assertThat(fetched.single.isLoaded()).isFalse();

		final HasSingle reloaded = ofy().load().group(HasSingle.Single.class).entity(hs).now();	// upgrade with single
		assertThat(reloaded.single.isLoaded()).isTrue();
	}

	/** */
	@Test
	void upgradingOutsideOfATransaction() throws Exception {
		factory().register(HasSingle.class);

		final HasSingle hs = new HasSingle();
		hs.single = Ref.create(ko0);
		final Key<HasSingle> hskey = ofy().save().entity(hs).now();

		ofy().clear();

		// Load hs in a transaction, which will then propagate back to parent session
		ofy().transact(() -> {
			ofy().load().key(hskey).now();
		});

		final HasSingle fetched = ofy().load().key(hskey).now();
		assertThat(fetched.single.isLoaded()).isFalse();

		final HasSingle reloaded = ofy().load().group(HasSingle.Single.class).key(hskey).now();	// upgrade with single
		assertThat(reloaded.single.isLoaded()).isTrue();
	}

	/** */
	@Test
	void reloadingOutsideOfATransaction() throws Exception {
		factory().register(HasSingle.class);

		final HasSingle hs = new HasSingle();
		hs.single = Ref.create(ko0);
		final Key<HasSingle> hskey = ofy().save().entity(hs).now();

		ofy().clear();

		// Load hs in a transaction, which will then propagate back to parent session, including the Ref async load
		ofy().transact(() -> {
			ofy().load().group(HasSingle.Single.class).key(hskey).now();
		});

		// This works even without the load group because we are loading the same object instance,
		// which has populated refs.  We don't unload refs.
		final HasSingle fetched = ofy().load().key(hskey).now();
		assertThat(fetched.single.isLoaded()).isTrue();
	}

	/** */
	@Test
	void reloadingOutsideOfATransaction2() throws Exception {
		factory().register(HasSingle.class);

		final HasSingle hs = new HasSingle();
		hs.single = Ref.create(ko0);
		final Key<HasSingle> hskey = ofy().save().entity(hs).now();

		ofy().clear();

		// Load hs in a transaction, which will then propagate back to parent session, including the Ref async load
		ofy().transact(() -> {
			ofy().load().group(HasSingle.Single.class).key(hskey).now();
		});

		// This is different by loading the group
		final HasSingle fetched = ofy().load().group(HasSingle.Single.class).key(hskey).now();
		assertThat(fetched.single.isLoaded()).isTrue();
	}
}