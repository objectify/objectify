package com.googlecode.objectify.test.util.valkey;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.EntityValue;
import com.google.cloud.datastore.FullEntity;
import com.google.cloud.datastore.IncompleteKey;
import com.google.cloud.datastore.Value;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.cache.MemcacheService;
import com.googlecode.objectify.impl.AsyncDatastore;
import com.googlecode.objectify.test.util.LocalDatastoreExtension;
import com.googlecode.objectify.test.util.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import static com.googlecode.objectify.ObjectifyService.factory;
import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * Mirror of {@link com.googlecode.objectify.test.util.TestBase} that wires the cache layer with
 * the Valkey-backed {@link com.googlecode.objectify.cache.valkey.ValkeyCacheService} instead of
 * spymemcached.
 */
@ExtendWith({
		MockitoExtension.class,
		LocalDatastoreExtension.class,
		LocalValkeyExtension.class,
		ValkeyObjectifyExtension.class,
})
public class ValkeyTestBase {
	protected Value<FullEntity<?>> makeEmbeddedEntityWithProperty(final String name, final Value<?> value) {
		return EntityValue.of(FullEntity.newBuilder().set(name, value).build());
	}

	protected Datastore datastore() {
		return factory().datastore();
	}

	protected MemcacheService memcache() {
		return factory().memcache();
	}

	protected AsyncDatastore asyncDatastore() {
		return factory().asyncDatastore();
	}

	protected <E> E saveClearLoad(final E thing) {
		final Key<E> key = ofy().save().entity(thing).now();
		ofy().clear();
		return ofy().load().key(key).now();
	}

	protected FullEntity.Builder<?> makeEntity(final Class<?> kind) {
		return makeEntity(Key.getKind(kind));
	}

	protected FullEntity.Builder<?> makeEntity(final String kind) {
		final IncompleteKey incompleteKey = factory().datastore().newKeyFactory().setKind(kind).newKey();
		return FullEntity.newBuilder(incompleteKey);
	}
}
