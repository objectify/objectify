package com.googlecode.objectify.test;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.LoadException;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Container;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.test.util.TestBase;
import lombok.Data;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static com.googlecode.objectify.ObjectifyService.factory;
import static com.googlecode.objectify.ObjectifyService.ofy;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 */
public class EmbeddedContainerTests extends TestBase {

	@Data
	private static class EmbedMe {
		@Container
		private HasEmbed container;
		private String foo;
	}

	@Entity
	@Cache
	private static class HasEmbed {
		@Id
		private Long id;
		private EmbedMe embedMe;
	}
	
	@Test
	void embedClassContainerPointsAtContainer() throws Exception {
		factory().register(HasEmbed.class);
		
		final HasEmbed he = new HasEmbed();
		he.embedMe = new EmbedMe();
		he.embedMe.foo = "bar";
		
		final HasEmbed fetched = saveClearLoad(he);
		assertThat(fetched.embedMe.container).isSameInstanceAs(fetched);
	}
	
	//
	//
	//

	@Data
	private static class SuperEmbedMe {
		@Container
		private Object container;
		private String foo;
	}

	@Entity
	@Cache
	private static class HasSuperEmbed {
		@Id Long id;
		SuperEmbedMe embedMe;
	}
	
	@Test
	void embedClassContainerPointsAtContainerWhenSpecifyingSuperclass() throws Exception {
		factory().register(HasSuperEmbed.class);
		
		final HasSuperEmbed he = new HasSuperEmbed();
		he.embedMe = new SuperEmbedMe();
		he.embedMe.foo = "bar";
		
		final HasSuperEmbed fetched = saveClearLoad(he);
		assertThat(fetched.embedMe.container).isSameInstanceAs(fetched);
	}
	
	//
	//
	//
	
	private static class DeepEmbedMe {
		@Container
		private NestedEmbedMe nestedContainer;
		@Container
		private HasNestedEmbed rootContainer;
		private String foo;
	}
	private static class NestedEmbedMe {
		@Container
		private HasNestedEmbed rootContainer;
		private DeepEmbedMe deep;
		private String foo;
	}
	@Entity
	@Cache
	private static class HasNestedEmbed {
		@Id
		private Long id;
		private NestedEmbedMe nested;
	}
	
	@Test
	void deepEmbedClassContainerPointsAtContainer() throws Exception {
		factory().register(HasNestedEmbed.class);
		
		final HasNestedEmbed he = new HasNestedEmbed();
		he.nested = new NestedEmbedMe();
		he.nested.foo = "bar";
		he.nested.deep = new DeepEmbedMe();
		he.nested.deep.foo = "bar";
		
		final HasNestedEmbed fetched = saveClearLoad(he);
		assertThat(fetched.nested.rootContainer).isSameInstanceAs(fetched);
		assertThat(fetched.nested.deep.rootContainer).isSameInstanceAs(fetched);
		assertThat(fetched.nested.deep.nestedContainer).isSameInstanceAs(fetched.nested);
	}
	
	//
	//
	//
	
	private static class BadEmbedMe {
		@Container
		private HasEmbed container;	// some other class!
		private String foo;
	}

	@Entity
	@Cache
	private static class BadHasEmbed {
		@Id Long id;
		BadEmbedMe embedMe;
	}

	/**
	 * We can't consistently detect this on registration because any class only gets turned
	 * into a translator once. It may be embedded in many other classes which don't have
	 * the correct container. So we just detect it on load.
	 */
	@Test
	void loadingBadContainerThrowsException() throws Exception {
		factory().register(BadHasEmbed.class);

		final BadHasEmbed bhe = new BadHasEmbed();
		bhe.embedMe = new BadEmbedMe();
		bhe.embedMe.foo = "bar";

		final Key<BadHasEmbed> key = ofy().save().entity(bhe).now();
		ofy().clear();

		assertThrows(LoadException.class, () -> ofy().load().key(key).now());
	}
}
