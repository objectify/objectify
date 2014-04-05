package com.googlecode.objectify.test;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Owner;
import com.googlecode.objectify.test.util.TestBase;
import org.testng.annotations.Test;

import static com.googlecode.objectify.test.util.TestObjectifyService.fact;
import static com.googlecode.objectify.test.util.TestObjectifyService.ofy;

/**
 */
public class EmbeddedOwnerTests extends TestBase
{
	public static class EmbedMe {
		@Owner HasEmbed owner;
		String foo;
	}
	@Entity
	@Cache
	public static class HasEmbed {
		@Id Long id;
		EmbedMe embedMe;
	}
	
	@Test
	public void embedClassOwnerPointsAtOwner() throws Exception {
		fact().register(HasEmbed.class);
		
		HasEmbed he = new HasEmbed();
		he.embedMe = new EmbedMe();
		he.embedMe.foo = "bar";
		
		HasEmbed fetched = ofy().putClearGet(he);
		assert fetched.embedMe.owner == fetched;
	}
	
	//
	//
	//
	
	public static class SuperEmbedMe {
		@Owner Object owner;
		String foo;
	}
	@Entity
	@Cache
	public static class HasSuperEmbed {
		@Id Long id;
		SuperEmbedMe embedMe;
	}
	
	@Test
	public void embedClassOwnerPointsAtOwnerWhenSpecifyingSuperclass() throws Exception {
		fact().register(HasSuperEmbed.class);
		
		HasSuperEmbed he = new HasSuperEmbed();
		he.embedMe = new SuperEmbedMe();
		he.embedMe.foo = "bar";
		
		HasSuperEmbed fetched = ofy().putClearGet(he);
		assert fetched.embedMe.owner == fetched;
	}
	
	//
	//
	//
	
	public static class DeepEmbedMe {
		@Owner NestedEmbedMe nestedOwner;
		@Owner HasNestedEmbed rootOwner;
		String foo;
	}
	public static class NestedEmbedMe {
		@Owner HasNestedEmbed rootOwner;
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
	public void deepEmbedClassOwnerPointsAtOwner() throws Exception {
		fact().register(HasNestedEmbed.class);
		
		HasNestedEmbed he = new HasNestedEmbed();
		he.nested = new NestedEmbedMe();
		he.nested.foo = "bar";
		he.nested.deep = new DeepEmbedMe();
		he.nested.deep.foo = "bar";
		
		HasNestedEmbed fetched = ofy().putClearGet(he);
		assert fetched.nested.rootOwner == fetched;
		assert fetched.nested.deep.rootOwner == fetched;
		assert fetched.nested.deep.nestedOwner == fetched.nested;
	}
	
	//
	//
	//
	
	public static class BadEmbedMe {
		@Owner HasEmbed owner;	// some other class!
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
	 * the correct owner. So we just detect it on load.
	 */
	@Test//(expectedExceptions=IllegalStateException.class)
	public void loadingBadOwnerThrowsException() throws Exception {
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
