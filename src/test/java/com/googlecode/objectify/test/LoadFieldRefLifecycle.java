/*
 */

package com.googlecode.objectify.test;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Ignore;
import com.googlecode.objectify.annotation.Load;
import com.googlecode.objectify.annotation.OnLoad;
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
 * Just some simple tests of loading field Refs
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
class LoadFieldRefLifecycle extends TestBase {
	/** */
	@Entity
	@Data
	@NoArgsConstructor
	private static class End {
		@Id long id;
		@Ignore boolean loaded = false;
		End(long id) { this.id = id; }

		@OnLoad void onLoad() {
			loaded = true;
		}
	}

	/** */
	@Entity
	@Data
	@NoArgsConstructor
	private static class Middle {
		@Id long id;
		@Load(HasSingle.Single.class) Ref<End> end;
		Middle(long id) { this.id = id; }
	}

	/** */
	@Entity
	private static class HasSingle {
		static class Single {}

		@Id Long id;
		@Load(Single.class) Ref<Middle> middle;
	}

	/** */
	@Entity
	private static class HasMulti {
		static class Multi {}

		@Id Long id;
		@Load(Multi.class) List<Ref<End>> ends = new ArrayList<>();
	}

	private Key<End> ke0;
	private End end0;
	private Key<End> ke1;
	private End end1;

	/** */
	@BeforeEach
	void createTwoOthers() {
		factory().register(End.class);

		end0 = new End(123L);
		ke0 = ofy().save().entity(end0).now();
		end1 = new End(456L);
		ke1 = ofy().save().entity(end1).now();
	}

	/** */
	@Test
	void testSingleOnLoad() throws Exception {
		factory().register(Middle.class);
		factory().register(HasSingle.class);

		final Middle mid = new Middle(456);
		mid.end = Ref.create(ke0);
		final Key<Middle> kmid = ofy().save().entity(mid).now();

		final HasSingle hs = new HasSingle();
		hs.middle = Ref.create(kmid);
		final Key<HasSingle> hskey = ofy().save().entity(hs).now();

		ofy().clear();
		//ofy().get(hskey);	// load once
		final HasSingle fetched = ofy().load().group(HasSingle.Single.class).key(hskey).now();	// upgrade with single

		assertThat(fetched.middle.get().end.get().loaded).isTrue();
	}

	/** */
	@Test
	void testMultiOnLoad() throws Exception {
		factory().register(HasMulti.class);

		final HasMulti hs = new HasMulti();
		hs.ends.add(Ref.create(ke0));
		hs.ends.add(Ref.create(ke1));
		final Key<HasMulti> hskey = ofy().save().entity(hs).now();

		ofy().clear();
		//ofy().get(hskey);	// load once
		final HasMulti fetched = ofy().load().group(HasMulti.Multi.class).key(hskey).now();	// upgrade with single

		for (Ref<End> end: fetched.ends)
			assertThat(end.get().loaded).isTrue();
	}
}