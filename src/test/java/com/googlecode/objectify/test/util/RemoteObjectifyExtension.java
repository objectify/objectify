package com.googlecode.objectify.test.util;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.testing.RemoteDatastoreHelper;
import com.google.common.base.Preconditions;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.util.Closeable;
import net.spy.memcached.MemcachedClient;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;

/**
 * Sets up and tears down the RemoteTestHelper, initializes objectify.
 * To use this, you will need GOOGLE_APPLICATION_CREDENTIALS set to a relevant service account key file.
 */
public class RemoteObjectifyExtension implements BeforeEachCallback, AfterEachCallback {

	private static final Namespace NAMESPACE = Namespace.create(RemoteObjectifyExtension.class);

	@Override
	public void beforeEach(final ExtensionContext context) throws Exception {
		final MemcachedClient memcachedClient = LocalMemcacheExtension.getClient(context);
		Preconditions.checkNotNull(memcachedClient, "This extension depends on " + LocalMemcacheExtension.class.getSimpleName());

		final RemoteDatastoreHelper helper = RemoteDatastoreHelper.create();
		context.getStore(NAMESPACE).put(RemoteDatastoreHelper.class, helper);

		final Datastore datastore = helper.getOptions().getService();

		ObjectifyService.init(new ObjectifyFactory(datastore, memcachedClient));

		final Closeable rootService = ObjectifyService.begin();

		context.getStore(NAMESPACE).put(Closeable.class, rootService);
	}

	@Override
	public void afterEach(final ExtensionContext context) throws Exception {
		final Closeable rootService = context.getStore(NAMESPACE).get(Closeable.class, Closeable.class);
		rootService.close();

		final RemoteDatastoreHelper helper = context.getStore(NAMESPACE).get(RemoteDatastoreHelper.class, RemoteDatastoreHelper.class);
		helper.deleteNamespace();
	}
}
