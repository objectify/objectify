/*
 */

package com.googlecode.objectify.test;

import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Load;
import com.googlecode.objectify.test.util.TestBase;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static com.googlecode.objectify.ObjectifyService.factory;
import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * Just some simple tests of loading field Refs
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
class LoadCollectionRefsTests extends TestBase {
	/** */
	@Entity
	@Data
	@NoArgsConstructor
	private static class Other {
		private @Id Long id;
		private Other(long id) { this.id = id; }
	}

	private static class SpecialList<T> extends ArrayList<Ref<T>> {
		private static final long serialVersionUID = 1L;

		@Override
		public boolean add(Ref<T> e) {
			//final T t = e.get();
			//System.out.println("ref contains " + t);
			return super.add(e);
		}
	}

	/** */
	@Entity
	@Data
	@NoArgsConstructor
	private static class Thing {
		private @Id Long id;
		private Thing(long id) { this.id = id; }

		@Load
		private List<Ref<Other>> others = new SpecialList<>();
	}

	/** */
	@BeforeEach
	void setUpThings() {
		factory().register(Thing.class);
		factory().register(Other.class);
	}

	/** */
	@Test
	void specialListWorks() throws Exception {
		final Other other = new Other();
		ofy().save().entity(other).now();

		final Thing thing = new Thing();
		thing.others.add(Ref.create(other));

		final Thing fetched = saveClearLoad(thing);
		assertThat(fetched.others).isEqualTo(thing.others);
	}
}