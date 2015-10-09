/*
 */

package com.googlecode.objectify.test;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Load;
import com.googlecode.objectify.test.entity.Trivial;
import com.googlecode.objectify.test.util.TestBase;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static com.googlecode.objectify.test.util.TestObjectifyService.fact;
import static com.googlecode.objectify.test.util.TestObjectifyService.ofy;

/**
 * Tests the fetching via queries
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class LoadQueryFieldTests extends TestBase
{
	Trivial t0;
	Trivial t1;
	Trivial tNone0;
	Trivial tNone1;
	Key<Trivial> k0;
	Key<Trivial> k1;
	Key<Trivial> kNone0;
	Key<Trivial> kNone1;

	/** */
	@BeforeMethod
	public void createTwo() {
		fact().register(Trivial.class);

		t0 = new Trivial("foo", 11);
		k0 = ofy().save().entity(t0).now();

		t1 = new Trivial("bar", 22);
		k1 = ofy().save().entity(t1).now();

		tNone0 = new Trivial(123L, "fooNone", 33);
		tNone1 = new Trivial(456L, "barNone", 44);

		kNone0 = Key.create(tNone0);
		kNone1 = Key.create(tNone1);
	}

	/** */
	@Entity
	public static class HasEntities {
		public @Id Long id;
		public @Load Ref<Trivial> single;
		public @Load List<Ref<Trivial>> multi = new ArrayList<>();
	}

	/** */
	@Test
	public void testTargetsExist() throws Exception
	{
		fact().register(HasEntities.class);

		HasEntities he = new HasEntities();
		he.single = Ref.create(t0);
		he.multi.add(Ref.create(t0));
		he.multi.add(Ref.create(t1));

		Key<HasEntities> hekey = ofy().save().entity(he).now();
		ofy().clear();

		HasEntities fetched = ofy().load().type(HasEntities.class).filterKey("=", hekey).first().now();

		assert fetched.single.get().getId().equals(t0.getId());
		assert fetched.single.get().getSomeString().equals(t0.getSomeString());

		assert fetched.multi.get(0).get() == fetched.single.get();

		assert fetched.multi.get(1).get().getId().equals(t1.getId());
		assert fetched.multi.get(1).get().getSomeString().equals(t1.getSomeString());
	}
}
