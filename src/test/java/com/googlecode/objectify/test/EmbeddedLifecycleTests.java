package com.googlecode.objectify.test;

import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Ignore;
import com.googlecode.objectify.annotation.OnLoad;
import com.googlecode.objectify.annotation.OnSave;
import com.googlecode.objectify.test.util.TestBase;
import org.testng.annotations.Test;

import static com.googlecode.objectify.test.util.TestObjectifyService.fact;
import static com.googlecode.objectify.test.util.TestObjectifyService.ofy;

/**
 * Tests the lifecycle annotations on embedded classes
 */
public class EmbeddedLifecycleTests extends TestBase
{
	@com.googlecode.objectify.annotation.Entity
	@Cache
	public static class Outer {
		@Id Long id;
		HasLifecycle life;
	}

	public static class HasLifecycle {
		@Ignore boolean onSaved;
		@Ignore boolean onLoaded;

		@OnSave void onSave() { this.onSaved = true; }
		@OnLoad void onLoad() { this.onLoaded = true; }
	}

	/** */
	@Test
	public void lifecycleInEmbeddedClassWorks() throws Exception {
		fact().register(Outer.class);

		Outer outer = new Outer();
		outer.life = new HasLifecycle();

		Outer fetched = ofy().saveClearLoad(outer);

		assert outer.life.onSaved;
		assert fetched.life.onLoaded;	// would fail without session clear
	}
}
