/*
 */

package com.googlecode.objectify.test;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.LoadResult;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Load;
import com.googlecode.objectify.test.util.TestBase;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static com.googlecode.objectify.ObjectifyService.factory;
import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * Tests the inheritance of load group classes
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
class LoadGroupInheritance extends TestBase {
	/** */
	@Entity
	@Data
	@NoArgsConstructor
	private static class Father {
		static class Bottom {}
		static class Middle extends Bottom {}
		static class Top extends Middle {}

		@Id long id;
		@Load(Middle.class) Ref<Child> child;

		Father(long id, Ref<Child> ch) {
			this.id = id;
			this.child = ch;
		}
	}

	/** */
	@Entity
	@Data
	@NoArgsConstructor
	private static class Child {
		@Id long id;

		Child(long id) { this.id = id; }
	}

	/** */
	@Test
	void testLoadNoGroup() throws Exception {
		factory().register(Father.class);
		factory().register(Child.class);

		final Child ch = new Child(123);
		final Key<Child> kch = ofy().save().entity(ch).now();

		final Father f = new Father(456, Ref.create(kch));
		final Key<Father> kf = ofy().save().entity(f).now();

		ofy().clear();
		final LoadResult<Father> fatherRef = ofy().load().key(kf);
		assertThat(fatherRef.now().child.isLoaded()).isFalse();
	}

	/** */
	@Test
	void testLoadBottomGroup() throws Exception {
		factory().register(Father.class);
		factory().register(Child.class);

		final Child ch = new Child(123);
		final Key<Child> kch = ofy().save().entity(ch).now();

		final Father f = new Father(456, Ref.create(kch));
		final Key<Father> kf = ofy().save().entity(f).now();

		ofy().clear();
		final LoadResult<Father> fatherRef = ofy().load().group(Father.Bottom.class).key(kf);
		assertThat(fatherRef.now().child.isLoaded()).isFalse();
	}

	/** */
	@Test
	void testLoadMiddleGroup() throws Exception {
		factory().register(Father.class);
		factory().register(Child.class);

		final Child ch = new Child(123);
		final Key<Child> kch = ofy().save().entity(ch).now();

		final Father f = new Father(456, Ref.create(kch));
		final Key<Father> kf = ofy().save().entity(f).now();

		ofy().clear();
		final LoadResult<Father> fatherRef = ofy().load().group(Father.Middle.class).key(kf);
		assertThat(fatherRef.now().child.get()).isNotNull();
	}

	/** */
	@Test
	void testLoadTopGroup() throws Exception {
		factory().register(Father.class);
		factory().register(Child.class);

		final Child ch = new Child(123);
		final Key<Child> kch = ofy().save().entity(ch).now();

		final Father f = new Father(456, Ref.create(kch));
		final Key<Father> kf = ofy().save().entity(f).now();

		ofy().clear();
		final LoadResult<Father> fatherRef = ofy().load().group(Father.Top.class).key(kf);
		assertThat(fatherRef.now().child.get()).isNotNull();
	}
}