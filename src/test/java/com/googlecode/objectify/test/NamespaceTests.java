/*
 */

package com.googlecode.objectify.test;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.NamespaceManager;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Namespace;
import com.googlecode.objectify.annotation.Parent;
import com.googlecode.objectify.cmd.QueryResultIterable;
import com.googlecode.objectify.test.entity.Trivial;
import com.googlecode.objectify.test.util.TestBase;
import com.googlecode.objectify.util.Closeable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static com.googlecode.objectify.ObjectifyService.factory;
import static com.googlecode.objectify.ObjectifyService.ofy;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests of the Namespace mechanism
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
class NamespaceTests extends TestBase {
	/** */
	@Test
	void keyCreateRespectsNamespace() throws Exception {

		final Key<Trivial> outside = Key.create(Trivial.class, 123);

		final Key<Trivial> inside;
		try (final Closeable ignored = NamespaceManager.set("foo")) {
			inside = Key.create(Trivial.class, 123);
		}

		assertThat(outside.getNamespace()).isEmpty();
		assertThat(inside.getNamespace()).isEqualTo("foo");
	}

	/** */
	@Test
	void namespaceManagerCanBeNested() throws Exception {

		final Key<Trivial> k1;
		final Key<Trivial> k2;
		final Key<Trivial> k3;

		try (final Closeable ignored = NamespaceManager.set("ns1")) {

			try (final Closeable ignored2 = NamespaceManager.set("ns2")) {
				k1 = Key.create(Trivial.class, 123);
			}

			k2 = Key.create(Trivial.class, 123);
		}

		k3 = Key.create(Trivial.class, 123);

		assertThat(k1.getNamespace()).isEqualTo("ns2");
		assertThat(k2.getNamespace()).isEqualTo("ns1");
		assertThat(k3.getNamespace()).isEqualTo("");
	}

	/** */
	@Test
	void childrenInheritParentNamespace() throws Exception {

		final Key<Trivial> inside;
		try (final Closeable ignored = NamespaceManager.set("foo")) {
			inside = Key.create(Trivial.class, 123);
		}

		final Key<Trivial> outside = Key.create(inside, Trivial.class, 456);

		assertThat(outside.getNamespace()).isEqualTo("foo");
	}

	/** */
	@Test
	void saveRespectsNamespace() throws Exception {
		factory().register(Trivial.class);

		final Key<Trivial> key;
		final Trivial triv;

		try (final Closeable ignored = NamespaceManager.set("foo")) {
			triv = new Trivial("foo", 5);
			key = ofy().save().entity(triv).now();
		}

		assertThat(key.getNamespace()).isEqualTo("foo");

		final Key<Trivial> withoutNamespace = Key.create(triv);

		assertThat(withoutNamespace).isNotEqualTo(key);
	}

	/** */
	@Test
	void queriesRespectNamespace() throws Exception {
		factory().register(Trivial.class);

		final Key<Trivial> key;
		final Trivial triv;

		try (final Closeable ignored = NamespaceManager.set("foo")) {
			triv = new Trivial("foo", 5);
			key = ofy().save().entity(triv).now();
		}

		final QueryResultIterable<Key<Trivial>> outside = ofy().load().type(Trivial.class).keys().iterable();
		assertThat(outside).isEmpty();

		try (final Closeable ignored = NamespaceManager.set("foo")) {
			final QueryResultIterable<Key<Trivial>> inside = ofy().load().type(Trivial.class).keys().iterable();
			assertThat(inside).containsExactly(key);
		}
	}

	/** */
	@Test
	void explicitNamespaceOverridesThreadLocalForQueries() throws Exception {
		factory().register(Trivial.class);

		final Key<Trivial> key;
		final Trivial triv;

		try (final Closeable ignored = NamespaceManager.set("foo")) {
			triv = new Trivial("foo", 5);
			key = ofy().save().entity(triv).now();
		}

		final QueryResultIterable<Key<Trivial>> q1 = ofy().namespace("foo").load().type(Trivial.class).keys().iterable();
		assertThat(q1).containsExactly(key);

		try (final Closeable ignored = NamespaceManager.set("bar")) {
			final QueryResultIterable<Key<Trivial>> q2 = ofy().load().type(Trivial.class).keys().iterable();
			assertThat(q2).isEmpty();

			final QueryResultIterable<Key<Trivial>> q3 = ofy().namespace("foo").load().type(Trivial.class).keys().iterable();
			assertThat(q3).containsExactly(key);
		}
	}

	/** */
	@Test
	void explicitNamespaceOverridesThreadLocalForKeyCreate() throws Exception {
		factory().register(Trivial.class);

		final Key<Trivial> key;
		final Trivial triv;

		try (final Closeable ignored = NamespaceManager.set("foo")) {
			triv = new Trivial("foo", 5);
			key = ofy().save().entity(triv).now();
		}

		final Trivial loaded0 = ofy().load().type(Trivial.class).id(key.getId()).now();
		assertThat(loaded0).isNull();

		final Trivial loaded1 = ofy().namespace("foo").load().type(Trivial.class).id(key.getId()).now();
		assertThat(loaded1).isEqualTo(triv);

		try (final Closeable ignored = NamespaceManager.set("bar")) {
			final Trivial loaded3 = ofy().load().type(Trivial.class).id(key.getId()).now();
			assertThat(loaded3).isNull();

			final Trivial loaded4 = ofy().namespace("foo").load().type(Trivial.class).id(key.getId()).now();
			assertThat(loaded4).isEqualTo(triv);
		}
	}

	@Entity
	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class Namespaced {
		@Namespace private String namespace;
		@Id private Long id;
	}

	/** */
	@Test
	void entityKeyHasNamespace() throws Exception {
		factory().register(Namespaced.class);

		final Namespaced ent = new Namespaced("foo", 123L);

		final Key<Namespaced> key = ofy().save().entity(ent).now();
		assertThat(key.getNamespace()).isEqualTo("foo");

		final Key<Namespaced> created = Key.create(ent);
		assertThat(created).isEqualTo(key);
	}

	/** */
	@Test
	void entityWithNamespaceLoads() throws Exception {
		factory().register(Namespaced.class);

		final Namespaced ent = new Namespaced("foo", 123L);
		final Key<Namespaced> key = ofy().save().entity(ent).now();

		ofy().clear();

		final Namespaced loaded = ofy().load().key(key).now();
		assertThat(loaded).isEqualTo(ent);

		final Namespaced not = ofy().load().type(Namespaced.class).id(123L).now();
		assertThat(not).isNull();

		final Namespaced loaded2 = ofy().namespace("foo").load().type(Namespaced.class).id(123L).now();
		assertThat(loaded2).isEqualTo(ent);

		try (final Closeable ignored = NamespaceManager.set("foo")) {
			final Namespaced loaded3 = ofy().load().type(Namespaced.class).id(123L).now();
			assertThat(loaded3).isEqualTo(ent);
		}

		final QueryResultIterable<Namespaced> loaded4 = ofy().load().type(Namespaced.class).iterable();
		assertThat(loaded4).isEmpty();
	}

	/** */
	@Test
	void entityWithNullNamespaceDefaultsToDefaultNamespace() throws Exception {
		factory().register(Namespaced.class);

		final Namespaced ent = new Namespaced(null, 123L);
		final Key<Namespaced> key = ofy().save().entity(ent).now();

		// Datastore default namespace is ""
		assertThat(key.getNamespace()).isEqualTo("");

		ofy().clear();

		final Namespaced expected = new Namespaced("", 123L);

		final Namespaced loaded = ofy().load().key(key).now();
		assertThat(loaded).isEqualTo(expected);

		final Namespaced loaded5 = ofy().load().type(Namespaced.class).id(123L).now();
		assertThat(loaded5).isEqualTo(expected);

		final Namespaced loaded2 = ofy().namespace("").load().type(Namespaced.class).id(123L).now();
		assertThat(loaded2).isEqualTo(expected);

		try (final Closeable ignored = NamespaceManager.set("")) {
			final Namespaced loaded3 = ofy().load().type(Namespaced.class).id(123L).now();
			assertThat(loaded3).isEqualTo(expected);
		}

		final QueryResultIterable<Namespaced> loaded4 = ofy().load().type(Namespaced.class).iterable();
		assertThat(loaded4).containsExactly(expected);
	}

	/** */
	@Test
	void entityWithNullNamespaceDefaultsToNamespaceManager() throws Exception {
		factory().register(Namespaced.class);

		final Namespaced ent = new Namespaced(null, 123L);
		final Key<Namespaced> key;

		try (final Closeable ignored = NamespaceManager.set("foo")) {
			key = ofy().save().entity(ent).now();
		}

		assertThat(key.getNamespace()).isEqualTo("foo");

		ofy().clear();

		final Namespaced expected = new Namespaced("foo", 123L);

		final Namespaced loaded = ofy().load().key(key).now();
		assertThat(loaded).isEqualTo(expected);

		final Namespaced not = ofy().load().type(Namespaced.class).id(123L).now();
		assertThat(not).isNull();

		final Namespaced loaded2 = ofy().namespace("foo").load().type(Namespaced.class).id(123L).now();
		assertThat(loaded2).isEqualTo(expected);

		try (final Closeable ignored = NamespaceManager.set("foo")) {
			final Namespaced loaded3 = ofy().load().type(Namespaced.class).id(123L).now();
			assertThat(loaded3).isEqualTo(expected);
		}

		final QueryResultIterable<Namespaced> loaded4 = ofy().load().type(Namespaced.class).iterable();
		assertThat(loaded4).isEmpty();
	}

	@Entity
	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class NamespacedParented {
		@Namespace private String namespace;
		@Parent private Key<Trivial> parent;
		@Id private Long id;
	}

	/** */
	@Test
	void canSaveWithMatchingNamespaceAndParent() throws Exception {
		factory().register(NamespacedParented.class);

		final Key<Trivial> parentKey = Key.create("foo", Trivial.class, 123L);

		final NamespacedParented thing = new NamespacedParented("foo", parentKey, 456L);

		final Key<NamespacedParented> key = ofy().save().entity(thing).now();

		assertThat(key).isEqualTo(Key.create(parentKey, NamespacedParented.class, 456L));
	}

	/** */
	@Test
	void parentKeyMustMatchNamespace() throws Exception {
		factory().register(NamespacedParented.class);

		final Key<Trivial> parentKey = Key.create("foo", Trivial.class, 123L);

		final NamespacedParented thing = new NamespacedParented("bar", parentKey, 456L);

		assertThrows(IllegalStateException.class, () -> {
			ofy().save().entity(thing).now();
		});
	}

	/** */
	@Test
	void parentKeyMustMatchInferredNamespace() throws Exception {
		factory().register(NamespacedParented.class);

		final Key<Trivial> parentKey = Key.create("foo", Trivial.class, 123L);

		final NamespacedParented thing = new NamespacedParented(null, parentKey, 456L);

		assertThrows(IllegalStateException.class, () -> {
			try (final Closeable ignored = NamespaceManager.set("bar")) {
				ofy().save().entity(thing).now();
			}
		});
	}

	/** */
	@Test
	void saveRespectsNamespaceUsingQueryOptions() throws Exception {
		factory().register(Trivial.class);

		final Trivial triv = new Trivial("foo", 5);

		final Key<Trivial> key = ofy().namespace("somens").save().entity(triv).now();
		assertThat(key.getNamespace()).isEqualTo("somens");

		final Trivial fetched = ofy().namespace("somens").load().type(Trivial.class).first().now();
		assertThat(fetched).isNotNull();
	}

}
