package com.googlecode.objectify.test.util.valkey;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.testing.LocalDatastoreHelper;
import com.google.common.base.Preconditions;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.cache.valkey.ValkeyCacheService;
import com.googlecode.objectify.test.util.LocalDatastoreExtension;
import com.googlecode.objectify.util.Closeable;
import glide.api.GlideClient;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;

/**
 * Equivalent of {@link com.googlecode.objectify.test.util.ObjectifyExtension} that wires the
 * factory with a {@link ValkeyCacheService} instead of the spymemcached-backed service.
 */
public class ValkeyObjectifyExtension implements BeforeEachCallback, AfterEachCallback {

	private static final Namespace NAMESPACE = Namespace.create(ValkeyObjectifyExtension.class);

	@Override
	public void beforeEach(final ExtensionContext context) throws Exception {
		final LocalDatastoreHelper helper = LocalDatastoreExtension.getHelper(context);
		Preconditions.checkNotNull(helper, "This extension depends on " + LocalDatastoreExtension.class.getSimpleName());

		final Datastore datastore = helper.getOptions().getService();

		final GlideClient client = LocalValkeyExtension.getClient(context);
		Preconditions.checkNotNull(client, "This extension depends on " + LocalValkeyExtension.class.getSimpleName());

		ObjectifyService.init(new ObjectifyFactory(datastore, new ValkeyCacheService(client)));

		final Closeable rootService = ObjectifyService.begin();

		context.getStore(NAMESPACE).put(Closeable.class, rootService);
	}

	@Override
	public void afterEach(final ExtensionContext context) throws Exception {
		final Closeable rootService = context.getStore(NAMESPACE).get(Closeable.class, Closeable.class);
		rootService.close();
	}
}
