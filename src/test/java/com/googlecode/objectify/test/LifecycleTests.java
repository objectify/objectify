package com.googlecode.objectify.test;

import com.googlecode.objectify.Ref;
import com.googlecode.objectify.SaveException;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Load;
import com.googlecode.objectify.annotation.OnLoad;
import com.googlecode.objectify.annotation.OnSave;
import com.googlecode.objectify.annotation.Parent;
import com.googlecode.objectify.test.entity.Trivial;
import com.googlecode.objectify.test.util.TestBase;
import org.testng.annotations.Test;

import static com.googlecode.objectify.test.util.TestObjectifyService.fact;
import static com.googlecode.objectify.test.util.TestObjectifyService.ofy;

/**
 * Tests the lifecycle annotations
 */
public class LifecycleTests extends TestBase
{
	@com.googlecode.objectify.annotation.Entity
	@Cache
	public static class HasLifecycle
	{
		@Id Long id;
		boolean onSaved;
		boolean onLoaded;

		@OnSave void onSave() { this.onSaved = true; }
		@OnLoad void onLoad() { this.onLoaded = true; }
	}

	@com.googlecode.objectify.annotation.Entity
	@Cache
	public static class HasInheritedLifecycle extends HasLifecycle {}

	/** */
	@Test
	public void testLifecycle() throws Exception {
		fact().register(HasLifecycle.class);
		fact().register(HasInheritedLifecycle.class);

		HasLifecycle life1 = new HasLifecycle();
		HasLifecycle fetched = ofy().saveClearLoad(life1);

		assert fetched.onSaved;
		assert fetched.onLoaded;	// would fail without session clear

		HasLifecycle life2 = new HasInheritedLifecycle();
		fetched = ofy().saveClearLoad(life2);

		assert fetched.onSaved;
		assert fetched.onLoaded;	// would fail without session clear
	}

	@com.googlecode.objectify.annotation.Entity
	@Cache
	public static class HasExceptionThrowingLifecycle
	{
		@Id Long id;
		@OnSave void onSave() { throw new UnsupportedOperationException(); }
	}

	/** */
	@Test(expectedExceptions = SaveException.class)
	public void testExceptionInLifecycle() throws Exception {
		fact().register(HasExceptionThrowingLifecycle.class);
		ofy().saveClearLoad(new HasExceptionThrowingLifecycle());
	}

	/** */
	@com.googlecode.objectify.annotation.Entity
	@Cache
	public static class HasLoad
	{
		@Id Long id;
		@Load Ref<Trivial> triv;
		@OnLoad void onLoad() {
			assert triv != null;
		}
	}

	/**
	 * Make sure that lifecycle methods are called after @Load happens
	 */
	@Test
	public void lifecycleMethodsAreCalledAfterLoadHappens() throws Exception {
		fact().register(HasLoad.class);
		fact().register(Trivial.class);

		Trivial triv = new Trivial("foo", 123);
		ofy().save().entity(triv).now();

		HasLoad hl = new HasLoad();
		hl.triv = Ref.create(triv);
		ofy().save().entity(hl).now();

		ofy().load().entity(hl).now();
	}

	/** */
	@com.googlecode.objectify.annotation.Entity
	@Cache
	public static class ParentThing
	{
		@Id Long id;
		String foo;
	}

	/** */
	@com.googlecode.objectify.annotation.Entity
	@Cache
	public static class HasParent
	{
		@Load @Parent Ref<ParentThing> parent;
		@Id Long id;
	}

	/** */
	@com.googlecode.objectify.annotation.Entity
	@Cache
	public static class HasHasParent
	{
		@Id Long id;
		@Load Ref<HasParent> hasParent;

		@OnLoad void onLoad() {
			assert hasParent.get().parent.get().foo.equals("fooValue");
		}
	}

	/**
	 * More complicated test of a more complicated structure
	 */
	@Test
	public void testComplicatedLifecycle() throws Exception {
		fact().register(ParentThing.class);
		fact().register(HasParent.class);
		fact().register(HasHasParent.class);

		ParentThing pt = new ParentThing();
		pt.foo = "fooValue";
		ofy().save().entity(pt).now();

		HasParent hp = new HasParent();
		hp.parent = Ref.create(pt);
		ofy().save().entity(hp).now();

		HasHasParent hhp = new HasHasParent();
		hhp.hasParent = Ref.create(hp);
		ofy().save().entity(hhp).now();

		ofy().load().entity(hhp).now();
	}
}
