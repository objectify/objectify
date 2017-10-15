package com.googlecode.objectify.test;

import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Ignore;
import com.googlecode.objectify.annotation.OnLoad;
import com.googlecode.objectify.annotation.OnSave;
import com.googlecode.objectify.test.util.TestBase;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static com.googlecode.objectify.ObjectifyService.factory;

/**
 * Tests the lifecycle annotations on embedded classes
 */
class EmbeddedLifecycleTests extends TestBase {

	@com.googlecode.objectify.annotation.Entity
	@Cache
	private static class Outer {
		@Id private Long id;
		private HasLifecycle life;
	}

	private static class HasLifecycle {
		@Ignore private boolean onSaved;
		@Ignore private boolean onLoaded;

		@OnSave void onSave() { this.onSaved = true; }
		@OnLoad void onLoad() { this.onLoaded = true; }
	}

	/** */
	@Test
	void lifecycleInEmbeddedClassWorks() throws Exception {
		factory().register(Outer.class);

		final Outer outer = new Outer();
		outer.life = new HasLifecycle();

		final Outer fetched = saveClearLoad(outer);

		assertThat(outer.life.onSaved).isTrue();
		assertThat(fetched.life.onLoaded).isTrue();	// would fail without session clear
	}
}
