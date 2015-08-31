package com.googlecode.objectify.test;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.test.entity.Trivial;
import com.googlecode.objectify.test.util.TestBase;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.testng.annotations.Test;
import static com.googlecode.objectify.test.util.TestObjectifyService.fact;
import static com.googlecode.objectify.test.util.TestObjectifyService.ofy;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Tests of embedding actual @Entity objects inside each other
 */
public class EmbeddedEntityTests extends TestBase
{
	@Entity
	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class Outer {
		private @Id Long id;
		private Trivial trivial;
	}

	@Test
	public void embeddedEntityPreservesKey() throws Exception {
		fact().register(Outer.class);
		fact().register(Trivial.class);

		final Outer outer = new Outer(123L, new Trivial(123L, "foo", 9));

		ofy().save().entity(outer).now();
		ofy().clear();
		final Outer fetched = ofy().load().entity(outer).now();

		assertThat(fetched, equalTo(outer));
	}

	@Test
	public void embeddedEntityAllowsNullKey() throws Exception {
		fact().register(Outer.class);
		fact().register(Trivial.class);

		final Outer outer = new Outer(123L, new Trivial(null, "foo", 9));

		ofy().save().entity(outer).now();
		ofy().clear();
		final Outer fetched = ofy().load().entity(outer).now();

		assertThat(fetched, equalTo(outer));
	}
}
