/*
 */

package com.googlecode.objectify.test;

import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Load;
import com.googlecode.objectify.test.util.TestBase;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static com.googlecode.objectify.test.util.TestObjectifyService.fact;
import static com.googlecode.objectify.test.util.TestObjectifyService.ofy;

/**
 * Just some simple tests of loading field Refs
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class LoadCollectionRefsTests extends TestBase
{
	/** */
	@Entity
	public static class Other {
		public @Id Long id;
		public Other() {}
		public Other(long id) { this.id = id; }
	}

	public static class SpecialList<T> extends ArrayList<Ref<T>> {
		private static final long serialVersionUID = 1L;

		@Override
		public boolean add(Ref<T> e) {
			T t = e.get();
			System.out.println("ref contains " + t);
			return super.add(e);
		}
	}

	/** */
	@Entity
	public static class Thing {
		public @Id Long id;
		public Thing() {}
		public Thing(long id) { this.id = id; }

		@Load
		public List<Ref<Other>> others = new SpecialList<Other>();
	}

	/** */
	@BeforeMethod
	public void setUpThings() {
		fact().register(Thing.class);
		fact().register(Other.class);
	}

	/** */
	@Test
	public void specialListWorks() throws Exception {
		Other other = new Other();
		ofy().save().entity(other).now();

		Thing thing = new Thing();
		thing.others.add(Ref.create(other));

		Thing fetched = ofy().putClearGet(thing);
		assert fetched.others.equals(thing.others);
	}
}