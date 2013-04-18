/*
 */

package com.googlecode.objectify.test;

import java.util.ArrayList;
import java.util.List;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Ignore;
import com.googlecode.objectify.annotation.Load;
import com.googlecode.objectify.annotation.OnLoad;
import com.googlecode.objectify.test.LoadFieldRefLifecycle.HasMulti.Multi;
import com.googlecode.objectify.test.LoadFieldRefLifecycle.HasSingle.Single;
import com.googlecode.objectify.test.util.TestBase;

import static com.googlecode.objectify.test.util.TestObjectifyService.fact;
import static com.googlecode.objectify.test.util.TestObjectifyService.ofy;

/**
 * Just some simple tests of loading field Refs
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class LoadFieldRefLifecycle extends TestBase
{
	/** */
	@Entity
	public static class End {
		public @Id long id;
		@Ignore boolean loaded = false;
		public End() {}
		public End(long id) { this.id = id; }

		public @OnLoad void onLoad() {
			loaded = true;
		}
	}

	/** */
	@Entity
	public static class Middle {
		public @Id long id;
		public Middle() {}
		public Middle(long id) { this.id = id; }

		public @Load(Single.class) Ref<End> end;
	}

	/** */
	@Entity
	public static class HasSingle {
		public static class Single {}

		public @Id Long id;
		public @Load(Single.class) Ref<Middle> middle;
	}

	/** */
	@Entity
	public static class HasMulti {
		public static class Multi {}

		public @Id Long id;
		public @Load(Multi.class) List<Ref<End>> ends = new ArrayList<Ref<End>>();
	}

	Key<End> ke0;
	End end0;
	Key<End> ke1;
	End end1;

	/** */
	@BeforeMethod
	public void createTwoOthers() {
		fact().register(End.class);

		end0 = new End(123L);
		ke0 = ofy().put(end0);
		end1 = new End(456L);
		ke1 = ofy().put(end1);
	}

	/** */
	@Test
	public void testSingleOnLoad() throws Exception
	{
		fact().register(Middle.class);
		fact().register(HasSingle.class);

		Middle mid = new Middle(456);
		mid.end = Ref.create(ke0);
		Key<Middle> kmid = ofy().put(mid);

		HasSingle hs = new HasSingle();
		hs.middle = Ref.create(kmid);
		Key<HasSingle> hskey = ofy().put(hs);

		ofy().clear();
		//ofy().get(hskey);	// load once
		HasSingle fetched = ofy().load().group(Single.class).key(hskey).get();	// upgrade with single

		assert fetched.middle.get().end.get().loaded;
	}

	/** */
	@Test
	public void testMultiOnLoad() throws Exception
	{
		fact().register(HasMulti.class);

		HasMulti hs = new HasMulti();
		hs.ends.add(Ref.create(ke0));
		hs.ends.add(Ref.create(ke1));
		Key<HasMulti> hskey = ofy().put(hs);

		ofy().clear();
		//ofy().get(hskey);	// load once
		HasMulti fetched = ofy().load().group(Multi.class).key(hskey).get();	// upgrade with single

		for (Ref<End> end: fetched.ends)
			assert end.get().loaded;
	}
}