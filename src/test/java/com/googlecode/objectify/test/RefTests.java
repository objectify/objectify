/*
 */

package com.googlecode.objectify.test;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.LoadResult;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Load;
import com.googlecode.objectify.test.entity.Trivial;
import com.googlecode.objectify.test.util.TestBase;
import lombok.Data;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import static com.google.common.truth.Truth.assertThat;
import static com.googlecode.objectify.ObjectifyService.factory;
import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * Tests the behavior of Refs.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
class RefTests extends TestBase {
	private Trivial t1;
	private Trivial t2;
	private Key<Trivial> k1;
	private Key<Trivial> k2;
	private Key<Trivial> kNone;

	/** */
	@BeforeEach
	void createTwo() {
		factory().register(Trivial.class);

		t1 = new Trivial("foo", 11);
		k1 = ofy().save().entity(t1).now();

		t2 = new Trivial("bar", 22);
		k2 = ofy().save().entity(t2).now();

		kNone = Key.create(Trivial.class, 12345L);

		ofy().clear();
	}

	/** */
	@Test
	void simpleRefIsNotLoaded() throws Exception {
		final Ref<Trivial> ref = Ref.create(k1);
		assertThat(ref.isLoaded()).isFalse();
	}

	/** */
	@Test
	void standaloneLoad() throws Exception {
		final Ref<Trivial> ref = Ref.create(k1);

		final Trivial loaded = ref.get();
		assertThat(ref.isLoaded()).isTrue();
		assertThat(loaded).isEqualTo(t1);
	}

	/** */
	@Test
	void loadRefFromOfy() throws Exception {
		final Ref<Trivial> ref = Ref.create(k1);

		final LoadResult<Trivial> result = ofy().load().ref(ref);
		assertThat(ref.isLoaded()).isTrue();
		assertThat(result.now()).isEqualTo(t1);
	}

	/** */
	@Test
	void getRefsVarargs() throws Exception {
		final Ref<Trivial> ref1 = Ref.create(k1);
		final Ref<Trivial> ref2 = Ref.create(k2);
		final Ref<Trivial> refNone = Ref.create(kNone);

		@SuppressWarnings({ "unused", "unchecked" })
		final Object foo = ofy().load().refs(ref1, ref2, refNone);

		assertThat(ref1.isLoaded()).isTrue();
		assertThat(ref2.isLoaded()).isTrue();
		assertThat(refNone.isLoaded()).isTrue();

		assertThat(ref1.get()).isEqualTo(t1);
		assertThat(ref2.get()).isEqualTo(t2);
		assertThat(refNone.get()).isNull();
	}

	/** */
	@Test
	void getRefsIterable() throws Exception {
		final Ref<Trivial> ref1 = Ref.create(k1);
		final Ref<Trivial> ref2 = Ref.create(k2);
		final Ref<Trivial> refNone = Ref.create(kNone);

		//noinspection unchecked
		ofy().load().refs(ref1, ref2, refNone);

		assertThat(ref1.isLoaded()).isTrue();
		assertThat(ref2.isLoaded()).isTrue();
		assertThat(refNone.isLoaded()).isTrue();

		assertThat(ref1.get()).isEqualTo(t1);
		assertThat(ref2.get()).isEqualTo(t2);
		assertThat(refNone.get()).isNull();
	}

	/** */
	@Entity
	@Data
	private static class HasRef implements Serializable {
		static final long serialVersionUID = 1L;
		static class Foo {}

		@Id Long id;
		@Load(Foo.class) Ref<Trivial> triv;
	}

	/** */
	@Test
	void refsMustBeSerializable() throws Exception {
		factory().register(HasRef.class);

		final HasRef hr = new HasRef();
		hr.triv = Ref.create(k1);

		final HasRef fetched = saveClearLoad(hr);
		final Object serialized = serializeDeserialize(fetched);

		assertThat(serialized).isEqualTo(hr);
	}

	private Object serializeDeserialize(final Object thing) throws IOException, ClassNotFoundException {
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		final ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.writeObject(thing);
		oos.close();

		final ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()));
		return ois.readObject();
	}

	/** */
	@Test
	void refsLoadedMustBeSerializable() throws Exception {
		factory().register(HasRef.class);

		final HasRef hr = new HasRef();
		hr.triv = Ref.create(k1);

		ofy().save().entity(hr).now();
		ofy().clear();
		final HasRef fetched = ofy().load().group(HasRef.Foo.class).entity(hr).now();
		final HasRef serialized = (HasRef)serializeDeserialize(fetched);
		assertThat(serialized).isEqualTo(hr);
		assertThat(serialized.triv.get()).isEqualTo(t1);
	}
}