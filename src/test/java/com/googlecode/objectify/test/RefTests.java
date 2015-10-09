/*
 */

package com.googlecode.objectify.test;

import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.LoadResult;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Load;
import com.googlecode.objectify.test.entity.Trivial;
import com.googlecode.objectify.test.util.TestBase;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static com.googlecode.objectify.test.util.TestObjectifyService.fact;
import static com.googlecode.objectify.test.util.TestObjectifyService.ofy;

/**
 * Tests the behavior of Refs.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class RefTests extends TestBase
{
	Trivial t1;
	Trivial t2;
	Key<Trivial> k1;
	Key<Trivial> k2;
	Key<Trivial> kNone;

	/** */
	@BeforeMethod
	public void createTwo() {
		fact().register(Trivial.class);

		t1 = new Trivial("foo", 11);
		k1 = ofy().save().entity(t1).now();

		t2 = new Trivial("bar", 22);
		k2 = ofy().save().entity(t2).now();

		kNone = Key.create(Trivial.class, 12345L);

		ofy().clear();
	}

//	/** */
//	@Test
//	public void testGet() throws Exception {
//		TestObjectify ofy = fact().begin();
//
//		Ref<Trivial> ref = Ref.create(k1);
//
//		ofy().getRef(ref);
//		assert ref.value().getSomeString().equals(t1);
//
//		try {
//			ofy().getRef(Ref.create(kNone));
//			assert false;
//		} catch (NotFoundException ex) {}
//	}

	/** */
	@Test
	public void simpleRefIsNotLoaded() throws Exception {
		Ref<Trivial> ref = Ref.create(k1);
		assert !ref.isLoaded();
	}

	/** */
	@Test
	public void standaloneLoad() throws Exception {
		Ref<Trivial> ref = Ref.create(k1);

		Trivial loaded = ref.get();
		assert ref.isLoaded();
		assert loaded.getSomeString().equals(t1.getSomeString());
	}

	/** */
	@Test
	public void loadRefFromOfy() throws Exception {
		Ref<Trivial> ref = Ref.create(k1);

		LoadResult<Trivial> result = ofy().load().ref(ref);
		assert ref.isLoaded();

		assert result.now().getSomeString().equals(t1.getSomeString());
	}

	/** */
	@Test
	public void testGetRefsVarargs() throws Exception {
		Ref<Trivial> ref1 = Ref.create(k1);
		Ref<Trivial> ref2 = Ref.create(k2);
		Ref<Trivial> refNone = Ref.create(kNone);

		@SuppressWarnings({ "unused", "unchecked" })
		Object foo = ofy().load().refs(ref1, ref2, refNone);

		assert ref1.isLoaded();
		assert ref2.isLoaded();
		assert refNone.isLoaded();

		assert ref1.get().getSomeString().equals(t1.getSomeString());
		assert ref2.get().getSomeString().equals(t2.getSomeString());
		assert refNone.get() == null;
	}

	/** */
	@Test
	public void testGetRefsIterable() throws Exception {
		Ref<Trivial> ref1 = Ref.create(k1);
		Ref<Trivial> ref2 = Ref.create(k2);
		Ref<Trivial> refNone = Ref.create(kNone);

		List<Ref<Trivial>> list = new ArrayList<>();
		list.add(ref1);
		list.add(ref2);
		list.add(refNone);

		ofy().load().refs(list);

		assert ref1.isLoaded();
		assert ref2.isLoaded();
		assert refNone.isLoaded();

		assert ref1.get().getSomeString().equals(t1.getSomeString());
		assert ref2.get().getSomeString().equals(t2.getSomeString());
		assert refNone.get() == null;
	}

	/** */
	@Entity
	static class HasRef implements Serializable {
		private static final long serialVersionUID = 1L;
		public static class Foo {}

		public @Id Long id;
		public @Load(Foo.class) Ref<Trivial> triv;
	}

	/** */
	@Test
	public void refsMustBeSerializable() throws Exception {
		fact().register(HasRef.class);

		HasRef hr = new HasRef();
		hr.triv = Ref.create(k1);

		HasRef fetched = ofy().saveClearLoad(hr);

		// Now try to serialize it in memcache.
		MemcacheService ms = MemcacheServiceFactory.getMemcacheService();
		ms.put("thing", fetched);

		HasRef serialized = (HasRef)ms.get("thing");
		assert serialized.id.equals(hr.id);
	}

	/** */
	@Test
	public void refsLoadedMustBeSerializable() throws Exception {
		fact().register(HasRef.class);

		HasRef hr = new HasRef();
		hr.triv = Ref.create(k1);

		ofy().save().entity(hr).now();
		ofy().clear();
		HasRef fetched = ofy().load().group(HasRef.Foo.class).entity(hr).now();

		// Now try to serialize it in memcache.
		MemcacheService ms = MemcacheServiceFactory.getMemcacheService();
		ms.put("thing", fetched);

		HasRef serialized = (HasRef)ms.get("thing");
		assert serialized.id.equals(hr.id);
		assert serialized.triv.get().getSomeString().equals(t1.getSomeString());
	}
}
