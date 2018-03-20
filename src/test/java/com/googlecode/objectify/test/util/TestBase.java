/*
 */

package com.googlecode.objectify.test.util;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.EntityValue;
import com.google.cloud.datastore.FullEntity;
import com.google.cloud.datastore.IncompleteKey;
import com.google.cloud.datastore.Value;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.cache.MemcacheService;
import com.googlecode.objectify.impl.AsyncDatastore;
import org.junit.jupiter.api.extension.ExtendWith;

import static com.googlecode.objectify.ObjectifyService.factory;
import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * All tests should extend this class to set up the GAE environment.
 * @see <a href="http://code.google.com/appengine/docs/java/howto/unittesting.html">Unit Testing in Appengine</a>
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
@ExtendWith({
		MockitoExtension.class,
		LocalDatastoreExtension.class,
		LocalMemcacheExtension.class,
		ObjectifyExtension.class,
})
public class TestBase {
	/** */
	protected Value<FullEntity<?>> makeEmbeddedEntityWithProperty(final String name, final Value<?> value) {
		return EntityValue.of(FullEntity.newBuilder().set(name, value).build());
	}

	/** */
	protected Datastore datastore() {
		return factory().datastore();
	}

	/** */
	protected MemcacheService memcache() {
		return factory().memcache();
	}

	protected AsyncDatastore asyncDatastore() {
		return factory().asyncDatastore();
	}

	/** */
	protected <E> E saveClearLoad(final E thing) {
		final Key<E> key = ofy().save().entity(thing).now();
		ofy().clear();
		return ofy().load().key(key).now();
	}

	/** */
	protected FullEntity.Builder<?> makeEntity(final Class<?> kind) {
		return makeEntity(Key.getKind(kind));
	}

	/** */
	protected FullEntity.Builder<?> makeEntity(final String kind) {
		final IncompleteKey incompleteKey = factory().datastore().newKeyFactory().setKind(kind).newKey();
		return FullEntity.newBuilder(incompleteKey);
	}
}
