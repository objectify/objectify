package com.googlecode.objectify.test.util;

/**
 * Sets up and tears down the Local Datastore emulator, uses eventual consistency
 */
public class LocalDatastoreExtensionEventual extends LocalDatastoreExtension {

	public LocalDatastoreExtensionEventual() {
		super(0.0);
	}
}
