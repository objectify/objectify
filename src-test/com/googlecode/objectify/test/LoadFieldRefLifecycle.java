/*
 */

package com.googlecode.objectify.test;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Ignore;
import com.googlecode.objectify.annotation.Load;
import com.googlecode.objectify.annotation.OnLoad;
import com.googlecode.objectify.test.LoadFieldRefLifecycle.HasSingle.Single;
import com.googlecode.objectify.test.util.TestBase;
import com.googlecode.objectify.test.util.TestObjectify;

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
	
	Key<End> ke0;
	End end0;
	
	/** */
	@BeforeMethod
	public void createTwoOthers() {
		fact.register(End.class);
		TestObjectify ofy = fact.begin();

		end0 = new End(123L);
		ke0 = ofy.put(end0);
	}

	/** */
	@Test
	public void testSingleReloaded() throws Exception
	{
		fact.register(Middle.class);
		fact.register(HasSingle.class);
		TestObjectify ofy = fact.begin();
		
		Middle mid = new Middle(456);
		mid.end = Ref.create(ke0);
		Key<Middle> kmid = ofy.put(mid);
		
		HasSingle hs = new HasSingle();
		hs.middle = Ref.create(kmid);
		Key<HasSingle> hskey = ofy.put(hs);
		
		ofy.clear();
		//ofy.get(hskey);	// load once
		HasSingle fetched = ofy.load().group(Single.class).key(hskey).get();	// upgrade with single

		assert fetched.middle.get().end.get().loaded;
	}

}