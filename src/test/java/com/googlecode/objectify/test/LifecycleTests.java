package com.googlecode.objectify.test;

import com.googlecode.objectify.Ref;
import com.googlecode.objectify.SaveException;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Load;
import com.googlecode.objectify.annotation.OnLoad;
import com.googlecode.objectify.annotation.OnSave;
import com.googlecode.objectify.annotation.Parent;
import com.googlecode.objectify.test.entity.Trivial;
import com.googlecode.objectify.test.util.TestBase;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static com.googlecode.objectify.ObjectifyService.factory;
import static com.googlecode.objectify.ObjectifyService.ofy;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests the lifecycle annotations
 */
class LifecycleTests extends TestBase {
	@Entity
	@Cache
	@Data
	private static class HasLifecycle {
		@Id Long id;
		boolean onSaved;
		boolean onLoaded;

		@OnSave void onSave() { this.onSaved = true; }
		@OnLoad void onLoad() { this.onLoaded = true; }
	}

	/** */
	@Test
	void lifecycleExecutes() throws Exception {
		factory().register(HasLifecycle.class);

		final HasLifecycle life1 = new HasLifecycle();
		final HasLifecycle fetched = saveClearLoad(life1);

		assertThat(fetched.onSaved).isTrue();
		assertThat(fetched.onLoaded).isTrue();	// would fail without session clear
	}

	@Entity
	@Cache
	private static class HasInheritedLifecycle extends HasLifecycle {}

	/** */
	@Test
	void inheritedLifecycleExecutes() throws Exception {
		factory().register(HasInheritedLifecycle.class);

		final HasInheritedLifecycle life2 = new HasInheritedLifecycle();
		final HasInheritedLifecycle fetched = saveClearLoad(life2);

		assertThat(fetched.onSaved).isTrue();
		assertThat(fetched.onLoaded).isTrue();	// would fail without session clear
	}

	@Entity
	@Cache
	@Data
	private static class HasExceptionThrowingLifecycle {
		@Id Long id;
		@OnSave void onSave() { throw new UnsupportedOperationException(); }
	}

	/** */
	@Test
	void testExceptionInLifecycle() throws Exception {
		factory().register(HasExceptionThrowingLifecycle.class);

		final HasExceptionThrowingLifecycle life = new HasExceptionThrowingLifecycle();
		assertThrows(SaveException.class, () -> ofy().save().entity(life).now());
	}

	/** */
	@Entity
	@Cache
	@Data
	private static class HasLoad {
		@Id Long id;
		@Load Ref<Trivial> triv;
		@OnLoad void onLoad() {
			assertThat(triv.isLoaded()).isTrue();
		}
	}

	/**
	 * Make sure that lifecycle methods are called after @Load happens
	 */
	@Test
	void lifecycleMethodsAreCalledAfterLoadHappens() throws Exception {
		factory().register(HasLoad.class);
		factory().register(Trivial.class);

		final Trivial triv = new Trivial("foo", 123);
		ofy().save().entity(triv).now();

		final HasLoad hl = new HasLoad();
		hl.triv = Ref.create(triv);
		ofy().save().entity(hl).now();

		ofy().load().entity(hl).now();
	}

	/** */
	@Entity
	@Cache
	@Data
	private static class ParentThing {
		@Id Long id;
		String foo;
	}

	/** */
	@Entity
	@Cache
	@Data
	private static class HasParent {
		@Load @Parent Ref<ParentThing> parent;
		@Id Long id;
	}

	/** */
	@Entity
	@Cache
	@Data
	private static class HasHasParent {
		@Id Long id;
		@Load Ref<HasParent> hasParent;

		@OnLoad void onLoad() {
			assertThat(hasParent.get().parent.get().foo).isEqualTo("fooValue");
		}
	}

	/**
	 * More complicated test of a more complicated structure
	 */
	@Test
	void testComplicatedLifecycle() throws Exception {
		factory().register(ParentThing.class);
		factory().register(HasParent.class);
		factory().register(HasHasParent.class);

		final ParentThing pt = new ParentThing();
		pt.foo = "fooValue";
		ofy().save().entity(pt).now();

		final HasParent hp = new HasParent();
		hp.parent = Ref.create(pt);
		ofy().save().entity(hp).now();

		final HasHasParent hhp = new HasHasParent();
		hhp.hasParent = Ref.create(hp);
		ofy().save().entity(hhp).now();

		ofy().load().entity(hhp).now();
	}

	@Entity
	@Cache
	@Data
	@EqualsAndHashCode(callSuper = true)
	private static class HasInheritedAndClassLifecycle extends HasLifecycle {
		@OnLoad void onLoadSubclass() {
			// The base class should have gotten called first
			assertThat(onLoaded).isTrue();
		}
		@OnSave void onSaveSubclass() {
			// The base class should have gotten called first
			assertThat(onSaved).isTrue();
		}
	}

	/** */
	@Test
	void inheritedLifecycleMethodsAreCalledInTheRightOrder() throws Exception {
		factory().register(HasInheritedAndClassLifecycle.class);

		final HasInheritedAndClassLifecycle life1 = new HasInheritedAndClassLifecycle();
		final HasInheritedAndClassLifecycle fetched = saveClearLoad(life1);

		assertThat(fetched.onSaved).isTrue();
		assertThat(fetched.onLoaded).isTrue();	// would fail without session clear
	}
}
