/*
 */

package com.googlecode.objectify.test;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.test.entity.Child;
import com.googlecode.objectify.test.entity.Trivial;
import com.googlecode.objectify.test.util.TestBase;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static com.googlecode.objectify.ObjectifyService.factory;
import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * Tests of ancestor relationships.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
class AncestorTests extends TestBase {
	/** */
	@Test
	void basicOperationsWithParentChild() throws Exception {
		factory().register(Trivial.class);
		factory().register(Child.class);

		final Trivial triv = new Trivial("foo", 5);
		final Key<Trivial> parentKey = ofy().save().entity(triv).now();

		final Child child = new Child(parentKey, "cry");
		final Key<Child> childKey = ofy().save().entity(child).now();

		assertThat(childKey.getParent()).isEqualTo(parentKey);

		final Child fetched = ofy().load().key(childKey).now();

		assertThat(fetched.getParent()).isEqualTo(child.getParent());
		assertThat(fetched.getChildString()).isEqualTo(child.getChildString());

		// Let's make sure we can get it back from an ancestor query
		final Child queried = ofy().load().type(Child.class).ancestor(parentKey).first().now();

		assertThat(queried.getParent()).isEqualTo(child.getParent());
		assertThat(queried.getChildString()).isEqualTo(child.getChildString());
	}
}