package com.googlecode.objectify.test;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.LoadException;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Container;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.test.util.TestBase;
import org.testng.annotations.Test;

import static com.googlecode.objectify.test.util.TestObjectifyService.fact;
import static com.googlecode.objectify.test.util.TestObjectifyService.ofy;

/**
 */
public class EmbeddedContainerTests extends TestBase
{
	public static class EmbedMe {
		@Container
		HasEmbed container;
		String foo;
	}

	@Entity
	@Cache
	public static class HasEmbed {
		@Id Long id;
		EmbedMe embedMe;
	}

	@Test
	public void embedClassContainerPointsAtContainer() throws Exception {
		fact().register(HasEmbed.class);

		HasEmbed he = new HasEmbed();
		he.embedMe = new EmbedMe();
		he.embedMe.foo = "bar";

		HasEmbed fetched = ofy().saveClearLoad(he);
		assert fetched.embedMe.container == fetched;
	}

	//
	//
	//

	public static class SuperEmbedMe {
		@Container
		Object container;
		String foo;
	}
	@Entity
	@Cache
	public static class HasSuperEmbed {
		@Id Long id;
		SuperEmbedMe embedMe;
	}

	@Test
	public void embedClassContainerPointsAtContainerWhenSpecifyingSuperclass() throws Exception {
		fact().register(HasSuperEmbed.class);

		HasSuperEmbed he = new HasSuperEmbed();
		he.embedMe = new SuperEmbedMe();
		he.embedMe.foo = "bar";

		HasSuperEmbed fetched = ofy().saveClearLoad(he);
		assert fetched.embedMe.container == fetched;
	}

	//
	//
	//

	public static class DeepEmbedMe {
		@Container
		NestedEmbedMe nestedContainer;
		@Container
		HasNestedEmbed rootContainer;
		String foo;
	}
	public static class NestedEmbedMe {
		@Container
		HasNestedEmbed rootContainer;
		DeepEmbedMe deep;
		String foo;
	}
	@Entity
	@Cache
	public static class HasNestedEmbed {
		@Id Long id;
		NestedEmbedMe nested;
	}

	@Test
	public void deepEmbedClassContainerPointsAtContainer() throws Exception {
		fact().register(HasNestedEmbed.class);

		HasNestedEmbed he = new HasNestedEmbed();
		he.nested = new NestedEmbedMe();
		he.nested.foo = "bar";
		he.nested.deep = new DeepEmbedMe();
		he.nested.deep.foo = "bar";

		HasNestedEmbed fetched = ofy().saveClearLoad(he);
		assert fetched.nested.rootContainer == fetched;
		assert fetched.nested.deep.rootContainer == fetched;
		assert fetched.nested.deep.nestedContainer == fetched.nested;
	}

	//
	//
	//

	public static class BadEmbedMe {
		@Container
		HasEmbed container;	// some other class!
		String foo;
	}

	@Entity
	@Cache
	public static class BadHasEmbed {
		@Id Long id;
		BadEmbedMe embedMe;
	}

	/**
	 * We can't consistently detect this on registration because any class only gets turned
	 * into a translator once. It may be embedded in many other classes which don't have
	 * the correct container. So we just detect it on load.
	 */
	@Test(expectedExceptions= LoadException.class)
	public void loadingBadContainerThrowsException() throws Exception {
		fact().register(BadHasEmbed.class);

		BadHasEmbed bhe = new BadHasEmbed();
		bhe.embedMe = new BadEmbedMe();
		bhe.embedMe.foo = "bar";

		Key<BadHasEmbed> key = ofy().save().entity(bhe).now();
		ofy().clear();

		// This should throw an exception
		ofy().load().key(key).now();
	}
}
