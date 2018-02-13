package com.googlecode.objectify.test.util;

import com.google.cloud.datastore.Datastore;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.util.Closeable;
import net.spy.memcached.MemcachedClient;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;

/**
 * Sets up and tears down the GAE local unit test harness environment
 */
public class ObjectifyExtension implements BeforeEachCallback, AfterEachCallback {

	private static final Namespace NAMESPACE = Namespace.create(ObjectifyExtension.class);

	@Override
	public void beforeEach(final ExtensionContext context) throws Exception {
		final Datastore datastore = LocalDatastoreExtension.getHelper(context).getOptions().getService();
		final MemcachedClient memcachedClient = LocalMemcacheExtension.getClient(context);

		ObjectifyService.init(new ObjectifyFactory(datastore, memcachedClient));

		final Closeable rootService = ObjectifyService.begin();

		context.getStore(NAMESPACE).put(Closeable.class, rootService);
	}

	@Override
	public void afterEach(final ExtensionContext context) throws Exception {
		final Closeable rootService = context.getStore(NAMESPACE).get(Closeable.class, Closeable.class);

		rootService.close();
	}
}
