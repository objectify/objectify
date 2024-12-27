package com.googlecode.objectify.test.util;

import com.google.api.gax.retrying.RetrySettings;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.QueryResults;
import com.google.cloud.datastore.StructuredQuery;
import com.google.cloud.http.HttpTransportOptions;

import java.util.UUID;

import org.threeten.bp.Duration;

/**
 * This class used to be part of the google SDK but was removed; it's been copied here instead.
 */
public class RemoteDatastoreHelper {
	private final DatastoreOptions options;
	private final Datastore datastore;
	private final String namespace;

	private RemoteDatastoreHelper(DatastoreOptions options) {
		this.options = options;
		this.datastore = options.getService();
		this.namespace = options.getNamespace();
	}

	/**
	 * Returns a {@link DatastoreOptions} object to be used for testing. The options are associated to
	 * a randomly generated namespace.
	 */
	public DatastoreOptions getOptions() {
		return options;
	}

	/**
	 * Deletes all entities in the namespace associated with this {@link RemoteDatastoreHelper}.
	 */
	public void deleteNamespace() {
		StructuredQuery<Key> query = Query.newKeyQueryBuilder().setNamespace(namespace).build();
		QueryResults<Key> keys = datastore.run(query);
		while (keys.hasNext()) {
			datastore.delete(keys.next());
		}
	}

	/**
	 * Creates a {@code RemoteStorageHelper} object.
	 */
	public static RemoteDatastoreHelper create() {
		return create("");
	}

	/**
	 * Creates a {@code RemoteStorageHelper} object.
	 */
	public static RemoteDatastoreHelper create(String databaseId) {
		HttpTransportOptions transportOptions = DatastoreOptions.getDefaultHttpTransportOptions();
		transportOptions =
			transportOptions.toBuilder().setConnectTimeout(60000).setReadTimeout(60000).build();
		DatastoreOptions.Builder datastoreOptionBuilder =
			DatastoreOptions.newBuilder()
				.setDatabaseId(databaseId)
				.setNamespace(UUID.randomUUID().toString())
				.setRetrySettings(retrySettings())
				.setTransportOptions(transportOptions);
		return new RemoteDatastoreHelper(datastoreOptionBuilder.build());
	}

	private static RetrySettings retrySettings() {
		return RetrySettings.newBuilder()
			.setMaxAttempts(10)
			.setMaxRetryDelay(Duration.ofMillis(30000L))
			.setTotalTimeout(Duration.ofMillis(120000L))
			.setInitialRetryDelay(Duration.ofMillis(250L))
			.setRetryDelayMultiplier(1.0)
			.setInitialRpcTimeout(Duration.ofMillis(120000L))
			.setRpcTimeoutMultiplier(1.0)
			.setMaxRpcTimeout(Duration.ofMillis(120000L))
			.build();
	}
}