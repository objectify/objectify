package com.googlecode.objectify.test.util;

import com.google.cloud.datastore.testing.LocalDatastoreHelper;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.threeten.bp.Duration;

/**
 * Sets up and tears down the Local Datastore emulator, defaults to strong consistency
 */
@RequiredArgsConstructor
public class LocalDatastoreExtension implements BeforeAllCallback, AfterAllCallback, BeforeEachCallback {

	private static final Namespace NAMESPACE = Namespace.create(LocalDatastoreExtension.class);

	private final double consistency;

	public LocalDatastoreExtension() {
		this(1.0);
	}

	@Override
	public void beforeAll(final ExtensionContext context) throws Exception {
		final LocalDatastoreHelper helper = LocalDatastoreHelper.create(consistency);
		context.getStore(NAMESPACE).put(LocalDatastoreHelper.class, helper);

		helper.start();
	}

	@Override
	public void afterAll(final ExtensionContext context) throws Exception {
		final LocalDatastoreHelper helper = context.getStore(NAMESPACE).get(LocalDatastoreHelper.class, LocalDatastoreHelper.class);
		helper.stop(Duration.ofSeconds(5));
	}

	@Override
	public void beforeEach(final ExtensionContext context) throws Exception {
		final LocalDatastoreHelper helper = context.getStore(NAMESPACE).get(LocalDatastoreHelper.class, LocalDatastoreHelper.class);
		helper.reset();
	}
}
