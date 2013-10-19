package com.googlecode.objectify.test;

import static com.googlecode.objectify.test.util.TestObjectifyService.fact;

import org.testng.annotations.Test;

import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Embed;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Owner;
import com.googlecode.objectify.test.util.TestBase;

/**
 */
public class EmbedOwnerTests extends TestBase
{
	@Embed
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
		
		HasEmbed fetched = putClearGet(he);
		assert fetched.embedMe.owner == fetched;
	}
	
	//
	//
	//
	
	@Embed
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
		
		HasSuperEmbed fetched = putClearGet(he);
		assert fetched.embedMe.owner == fetched;
	}
	
	//
	//
	//
	
	@Embed
	public static class DeepEmbedMe {
		@Owner NestedEmbedMe nestedOwner;
		@Owner HasNestedEmbed rootOwner;
		String foo;
	}
	@Embed
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
		
		HasNestedEmbed fetched = putClearGet(he);
		assert fetched.nested.rootOwner == fetched;
		assert fetched.nested.deep.rootOwner == fetched;
		assert fetched.nested.deep.nestedOwner == fetched.nested;
	}
	
	//
	//
	//
	
	@Embed
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

	@Test(expectedExceptions=IllegalStateException.class)
	public void registeringBadOwnerFieldThrowsException() throws Exception {
		fact().register(BadHasEmbed.class);
	}
}
