/*
 */

package com.googlecode.objectify.test;

import com.google.cloud.datastore.Entity;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Parent;
import com.googlecode.objectify.cache.CachingAsyncDatastore;
import com.googlecode.objectify.cache.EntityMemcache;
import com.googlecode.objectify.impl.AsyncDatastore;
import com.googlecode.objectify.impl.AsyncDatastoreImpl;
import com.googlecode.objectify.impl.AsyncTransaction;
import com.googlecode.objectify.test.util.TestBase;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static com.googlecode.objectify.ObjectifyService.factory;

/**
 * Tests of a bizarre bug in Google's memcache serialization of Key objects.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
class EvilMemcacheBugTests extends TestBase {

	/** */
	@com.googlecode.objectify.annotation.Entity
	@Data
	@NoArgsConstructor
	private static class SimpleParent {
		@Id String id;

		SimpleParent(String id) {
			this.id = id;
		}

		static Key<SimpleParent> getSimpleParentKey(String id) {
			return Key.create(SimpleParent.class, id);
		}
	}

	/** */
	@com.googlecode.objectify.annotation.Entity
	@Cache
	@Data
	@NoArgsConstructor
	private static class SimpleEntity {
		@Parent Key<SimpleParent> simpleParentKey;
		@Id String id;

		String foo = "bar";

		static Key<SimpleEntity> getSimpleChildKey(String id) {
			return Key.create(SimpleParent.getSimpleParentKey(id), SimpleEntity.class, id);
		}

		SimpleEntity(String id) {
			this.id = id;
			this.simpleParentKey = SimpleParent.getSimpleParentKey(id);
		}
	}

	/** */
	@Test
	void testRawTransactionalCaching() throws Exception {
		// Need to register it so the entity kind becomes cacheable
		factory().register(SimpleEntity.class);

		final EntityMemcache mc = new EntityMemcache(memcache(), "somenamespace");
		final AsyncDatastore cacheds = new CachingAsyncDatastore(new AsyncDatastoreImpl(datastore()), mc);

		// This is the weirdest thing.  If you change the *name* of one of these two keys, the test passes.
		// If the keys have the same *name*, the test fails because ent3 has the "original" property.  WTF??
		final com.google.cloud.datastore.Key parentKey = datastore().newKeyFactory().setKind("SimpleParent").newKey("asdf");
		final com.google.cloud.datastore.Key childKey = com.google.cloud.datastore.Key.newBuilder(parentKey, "SimpleEntity", "asdf").build();

		final Entity ent1 = Entity.newBuilder(childKey).set("foo", "original").build();
		cacheds.put(ent1);

		// Weirdly, this will solve the problem too
		//MemcacheService cs = MemcacheServiceFactory.getMemcacheService();
		//cs.clearAll();

		final AsyncTransaction txn = cacheds.newTransaction(() -> {});
		try {
			final Entity ent2 = txn.get(childKey).get().values().iterator().next();
			//ent2 = new Entity(childKey);	// solves the problem

			final Entity ent2a = Entity.newBuilder(ent2).set("foo", "changed").build();
			txn.put(ent2a);
			txn.commit();
		} finally {
			if (txn.isActive())
				txn.rollback();
		}

		final Entity ent3 = cacheds.get(childKey).get().values().iterator().next();

		assertThat(ent3.getString("foo")).isEqualTo("changed");
	}

	/** */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	void testRawCaching() throws Exception {
		// I can not for the life of me figure out why this test passes when the
		// previous test fails.

//		final MemcacheService cs1 = MemcacheServiceFactory.getMemcacheService("blah");
//
//		final com.google.cloud.datastore.Key parentKey = KeyFactory.createKey("SimpleParent", "asdf");
//		final com.google.cloud.datastore.Key childKey = KeyFactory.createKey(parentKey, "SimpleEntity", "asdf");
//
//		final Entity ent = new Entity(childKey);
//		ent.setProperty("foo", "original");
//		cs1.put(childKey, ent);
//
//		datastore().put(ent);
//
//		final Transaction txn = datastore().beginTransaction();
//		try {
//			final Entity ent2 = datastore().get(txn, childKey);
//
//			//Entity ent2 = (Entity)cs1.get(childKey);
//			assertThat(ent2.getProperty("foo")).isEqualTo("original");
//			ent2.setProperty("foo", "changed");
//
//			final Map<Object, Object> holder = new HashMap<>();
//			holder.put(childKey, ent2);
//			cs1.putAll(holder);
//
//			final Map<Object, Object> fetched = cs1.getAll((Collection)Collections.singleton(childKey));
//			final Entity ent3 = (Entity)fetched.get(childKey);
//			assertThat(ent3.getProperty("foo")).isEqualTo("changed");
//		} finally {
//			if (txn.isActive())
//				txn.rollback();
//		}

		throw new UnsupportedOperationException("TODO fix this test");
	}

}