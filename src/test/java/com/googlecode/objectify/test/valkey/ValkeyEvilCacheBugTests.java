package com.googlecode.objectify.test.valkey;

import com.google.cloud.datastore.Entity;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.TxnOptions;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Parent;
import com.googlecode.objectify.cache.CachingAsyncDatastore;
import com.googlecode.objectify.cache.EntityMemcache;
import com.googlecode.objectify.impl.AsyncDatastore;
import com.googlecode.objectify.impl.AsyncDatastoreImpl;
import com.googlecode.objectify.impl.AsyncTransaction;
import com.googlecode.objectify.test.util.valkey.ValkeyTestBase;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.google.common.truth.Truth.assertThat;
import static com.googlecode.objectify.ObjectifyService.factory;

/**
 * Valkey-backed mirror of {@link com.googlecode.objectify.test.EvilMemcacheBugTests}. The original
 * test caught a regression where in-flight transaction state polluted the cache; we re-run it
 * against the Valkey backend to ensure the CAS path doesn't reintroduce the issue.
 */
class ValkeyEvilCacheBugTests extends ValkeyTestBase {

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

	@Test
	void testRawTransactionalCaching() throws Exception {
		factory().register(SimpleEntity.class);

		final EntityMemcache mc = new EntityMemcache(memcache(), "somenamespace");
		final AsyncDatastore cacheds = new CachingAsyncDatastore(new AsyncDatastoreImpl(datastore()), mc);

		final com.google.cloud.datastore.Key parentKey = datastore().newKeyFactory().setKind("SimpleParent").newKey("asdf");
		final com.google.cloud.datastore.Key childKey = com.google.cloud.datastore.Key.newBuilder(parentKey, "SimpleEntity", "asdf").build();

		final Entity ent1 = Entity.newBuilder(childKey).set("foo", "original").build();
		cacheds.put(ent1);

		final AsyncTransaction txn = cacheds.newTransaction(TxnOptions.deflt(), () -> {}, Optional.empty());
		try {
			final Entity ent2 = txn.get(childKey).get().values().iterator().next();

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
}
