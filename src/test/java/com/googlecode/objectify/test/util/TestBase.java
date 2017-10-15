/*
 */

package com.googlecode.objectify.test.util;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.EmbeddedEntity;
import com.googlecode.objectify.Key;
import org.junit.jupiter.api.extension.ExtendWith;

import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * All tests should extend this class to set up the GAE environment.
 * @see <a href="http://code.google.com/appengine/docs/java/howto/unittesting.html">Unit Testing in Appengine</a>
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
@ExtendWith({
		MockitoExtension.class,
		GAEExtension.class,
		ObjectifyExtension.class,
})
public class TestBase {
	/** */
	protected EmbeddedEntity makeEmbeddedEntityWithProperty(String name, Object value) {
		EmbeddedEntity emb = new EmbeddedEntity();
		emb.setProperty(name, value);
		return emb;
	}

	/** */
	protected DatastoreService ds() {
		return DatastoreServiceFactory.getDatastoreService();
	}

	/** */
	protected <E> E saveClearLoad(final E thing) {
		final Key<E> key = ofy().save().entity(thing).now();
		ofy().clear();
		return ofy().load().key(key).now();
	}
}
