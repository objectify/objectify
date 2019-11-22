/*
 */

package com.googlecode.objectify.test;

import com.google.cloud.datastore.Blob;
import com.google.cloud.datastore.DatastoreException;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.LoadResult;
import com.googlecode.objectify.SaveException;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.Parent;
import com.googlecode.objectify.test.entity.Employee;
import com.googlecode.objectify.test.entity.NamedTrivial;
import com.googlecode.objectify.test.entity.Trivial;
import com.googlecode.objectify.test.util.TestBase;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.google.common.truth.Truth.assertThat;
import static com.googlecode.objectify.ObjectifyService.factory;
import static com.googlecode.objectify.ObjectifyService.ofy;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests of basic entity manipulation.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
class BasicTests extends TestBase {
	/** */
	@Test
	void idIsGenerated() throws Exception {
		factory().register(Trivial.class);

		// Note that 5 is not the id, it's part of the payload
		final Trivial triv = new Trivial("foo", 5);
		final Key<Trivial> k = ofy().save().entity(triv).now();

		assertThat(k.getKind()).isEqualTo(triv.getClass().getSimpleName());
		assertThat(k.getId()).isEqualTo(triv.getId());
	}
	
	/** */
	@Test
	void savingNullNamedIdThrowsException() throws Exception {
		factory().register(NamedTrivial.class);
		
		final NamedTrivial triv = new NamedTrivial(null, "foo", 5);

		assertThrows(SaveException.class, () -> {
			ofy().save().entity(triv).now();
		});
	}

	/** */
	@Test
	void savingEntityWithSameIdOverwritesData() throws Exception {
		factory().register(Trivial.class);

		final Trivial triv = new Trivial("foo", 5);
		final Key<Trivial> k = ofy().save().entity(triv).now();

		final Trivial triv2 = new Trivial(k.getId(), "bar", 6);
		final Key<Trivial> k2 = ofy().save().entity(triv2).now();

		assertThat(k2).isEqualTo(k);

		final Trivial fetched = ofy().load().key(k).now();
		assertThat(fetched).isSameInstanceAs(triv2);

		ofy().clear();
		final Trivial fetched2 = ofy().load().key(k).now();
		assertThat(fetched2.getId()).isEqualTo(k.getId());
		assertThat(fetched2.getSomeNumber()).isEqualTo(triv2.getSomeNumber());
		assertThat(fetched2.getSomeString()).isEqualTo(triv2.getSomeString());
	}

	/** */
	@Test
	void savingEntityWithNameIdWorks() throws Exception {
		factory().register(NamedTrivial.class);

		final NamedTrivial triv = new NamedTrivial("first", "foo", 5);
		final Key<NamedTrivial> k = ofy().save().entity(triv).now();

		assertThat(k.getName()).isEqualTo("first");
		assertThat(k).isEqualTo(Key.create(NamedTrivial.class, "first"));

		final NamedTrivial fetched = ofy().load().key(k).now();
		assertThat(fetched).isSameInstanceAs(triv);

		ofy().clear();
		final NamedTrivial fetched2 = ofy().load().key(k).now();
		assertThat(fetched2.getName()).isEqualTo(k.getName());
		assertThat(fetched2.getSomeNumber()).isEqualTo(triv.getSomeNumber());
		assertThat(fetched2.getSomeString()).isEqualTo(triv.getSomeString());
	}

	/** */
	@Test
	void simpleBatchOperations() throws Exception {
		factory().register(Trivial.class);

		final Trivial triv1 = new Trivial("foo", 5);
		final Trivial triv2 = new Trivial("foo2", 6);
		final List<Trivial> objs = Arrays.asList(triv1, triv2);

		final Map<Key<Trivial>, Trivial> map = ofy().save().entities(objs).now();
		final List<Key<Trivial>> keys = new ArrayList<>(map.keySet());

		assertThat(keys.stream().map(Key::getId).collect(Collectors.toList()))
				.containsExactlyElementsIn(objs.stream().map(Trivial::getId).collect(Collectors.toList())).inOrder();

		final Map<Key<Trivial>, Trivial> fetched = ofy().load().keys(keys);

		assertThat(fetched.values()).containsExactlyElementsIn(objs).inOrder();
	}

	/** */
	@Test
	void queryByKeyFieldUsingEntityObject() throws Exception {
		factory().register(Employee.class);

		final Employee fred = new Employee("fred");
		ofy().save().entity(fred).now();

		final Key<Employee> fredKey = Key.create(fred);

		final List<Employee> employees = new ArrayList<>(100);
		for (int i = 0; i < 100; i++) {
			final Employee emp = new Employee("foo" + i, fredKey);
			employees.add(emp);
		}

		ofy().save().entities(employees).now();

		final List<Employee> subordinates = ofy().load().type(Employee.class).filter("manager", fred).list();
		assertThat(subordinates).hasSize(100);
	}

	/** */
	@Test
	void keyToStringAndBack() throws Exception {
		final Key<Trivial> trivKey = Key.create(Trivial.class, 123);

		final String stringified = trivKey.toWebSafeString();

		final Key<Trivial> andBack = Key.create(stringified);

		assertThat(andBack).isEqualTo(trivKey);
	}

	/** */
	@Test
	void putEmptyListDoesNothing() throws Exception {
		ofy().save().entities(Collections.emptyList()).now();
	}

	/** */
	@Test
	void entitiesCanBeDeletedInBatch() throws Exception {
		factory().register(Trivial.class);

		final Trivial triv1 = new Trivial("foo5", 5);
		final Trivial triv2 = new Trivial("foo6", 6);

		ofy().save().entities(triv1, triv2).now();

		assertThat(ofy().load().entities(triv1, triv2)).hasSize(2);

		ofy().delete().entities(triv1, triv2).now();

		final Map<Key<Trivial>, Trivial> result = ofy().load().entities(triv1, triv2);
		assertThat(result).isEmpty();
	}

	/** */
	@Test
	void entitiesCanBeDeletedInBatchOfPrimitiveIds() throws Exception {
		factory().register(Trivial.class);

		final Trivial triv1 = new Trivial("foo5", 5);
		final Trivial triv2 = new Trivial("foo6", 6);
		triv1.setId(12345L);
		triv2.setId(12346L);

		ofy().save().entities(triv1, triv2).now();

		assertThat(ofy().load().entities(triv1, triv2)).hasSize(2);

		ofy().delete().type(Trivial.class).ids(12345L, 12346L).now();

		final Map<Key<Trivial>, Trivial> result = ofy().load().entities(triv1, triv2);
		assertThat(result).isEmpty();
	}

	/** */
	@Test
	void loadWithManuallyCreatedKey() throws Exception {
		factory().register(Trivial.class);

		final Trivial triv1 = new Trivial(123L, "foo5", 5);
		ofy().save().entity(triv1).now();

		final Trivial fetched = ofy().load().key(Key.create(Trivial.class, 123L)).now();
		assertThat(fetched).isSameInstanceAs(triv1);

		ofy().clear();
		final Trivial fetched2 = ofy().load().key(Key.create(Trivial.class, 123L)).now();
		assertThat(fetched2).isNotSameInstanceAs(triv1);
		assertThat(fetched2).isEqualTo(triv1);
	}

	/** */
	@Test
	void loadNonexistant() throws Exception {
		factory().register(Trivial.class);

		final Trivial triv1 = new Trivial("foo5", 5);
		ofy().save().entity(triv1).now();

		final Key<Trivial> triv1Key = Key.create(triv1);
		final Key<Trivial> triv2Key = Key.create(Trivial.class, 998);
		final Key<Trivial> triv3Key = Key.create(Trivial.class, 999);

		final LoadResult<Trivial> res = ofy().load().key(triv2Key);
		assertThat(res.now()).isNull();

		@SuppressWarnings("unchecked")
		final Map<Key<Trivial>, Trivial> result = ofy().load().keys(triv2Key, triv3Key);
		assertThat(result).isEmpty();

		@SuppressWarnings("unchecked")
		final Map<Key<Trivial>, Trivial> result2 = ofy().load().keys(triv1Key, triv2Key);
		assertThat(result2).hasSize(1);
	}

	/** */
	@Test
	void loadNonexistantWithoutSession() throws Exception {
		factory().register(Trivial.class);

		final Trivial triv1 = new Trivial("foo5", 5);
		ofy().save().entity(triv1).now();

		final Key<Trivial> triv1Key = Key.create(triv1);
		final Key<Trivial> triv2Key = Key.create(Trivial.class, 998);
		final Key<Trivial> triv3Key = Key.create(Trivial.class, 999);

		ofy().clear();
		final LoadResult<Trivial> res = ofy().load().key(triv2Key);
		assertThat(res.now()).isNull();

		ofy().clear();
		@SuppressWarnings("unchecked")
		final Map<Key<Trivial>, Trivial> result = ofy().load().keys(triv2Key, triv3Key);
		assertThat(result).isEmpty();

		ofy().clear();
		@SuppressWarnings("unchecked")
		final Map<Key<Trivial>, Trivial> result2 = ofy().load().keys(triv1Key, triv2Key);
		assertThat(result2).hasSize(1);
	}

	/** */
	@Test
	void simpleFetchById() throws Exception {
		factory().register(Trivial.class);

		final Trivial triv1 = new Trivial("foo5", 5);

		ofy().save().entity(triv1).now();
		ofy().clear();

		final Trivial fetched = ofy().load().type(Trivial.class).id(triv1.getId()).now();

		assertThat(fetched).isEqualTo(triv1);
	}
	
	
	@Entity
	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	private static class HasParent {
		@Parent
		private Key<Trivial> parent;

		@Id
		private long id;
	}
	
	@Test
	void deleteAnEntityWithAParent() throws Exception {
		factory().register(Trivial.class);
		factory().register(HasParent.class);
		
		final HasParent hp = new HasParent(Key.create(Trivial.class, 123L), 456L);

		final Key<HasParent> hpKey = ofy().save().entity(hp).now();
		ofy().clear();
		assertThat(ofy().load().key(hpKey).now()).isNotNull();

		ofy().delete().entity(hp).now();
		ofy().clear();
		assertThat(ofy().load().key(hpKey).now()).isNull();
	}

	@Entity
	@Data
	private static class HasIndexedBlob {
		@Id
		private Long id;

		@Index
		private Blob blob;
	}

	/**
	 * This isn't much of a test anymore; it used to be that blobs couldn't be indexed, now it's just 1500 chars
	 * that can be indexed.
	 */
	@Test
	void saveAnIndexedBlob() throws Exception {
		factory().register(HasIndexedBlob.class);

		final byte[] stuff = "asdf".getBytes();

		final HasIndexedBlob hib = new HasIndexedBlob();
		hib.blob = Blob.copyFrom(stuff);

		final HasIndexedBlob fetched = saveClearLoad(hib);
		final byte[] fetchedStuff = fetched.blob.toByteArray();

		assertThat(fetchedStuff).isEqualTo(stuff);
	}

	@Test
	void saveAnIndexedBlobThatIsTooLargeToBeIndexed() throws Exception {
		factory().register(HasIndexedBlob.class);

		final byte[] stuff = new byte[1501];
		Arrays.fill(stuff, (byte)42);

		final HasIndexedBlob hib = new HasIndexedBlob();
		hib.blob = Blob.copyFrom(stuff);

		// Blobs used to never be indexed but now they can. Caveat is that if it's bigger than 1500 bytes it will
		// throw an exception. We could simply skip indexing of larger blobs but that seems odd, so we'll follow the
		// underlying SDK behavior and let the exception propagate.
		assertThrows(DatastoreException.class, () -> {
			final HasIndexedBlob fetched = saveClearLoad(hib);
			// Old behavior
//			final byte[] fetchedStuff = fetched.blob.toByteArray();
//			assertThat(fetchedStuff).isEqualTo(stuff);
		}, "The value of property \"blob\" is longer than 1500 bytes.");
	}
}