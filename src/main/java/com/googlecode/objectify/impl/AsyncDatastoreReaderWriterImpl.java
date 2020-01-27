package com.googlecode.objectify.impl;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreReaderWriter;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.FullEntity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.QueryResults;
import com.google.cloud.datastore.ReadOption;
import com.google.common.collect.Iterables;
import com.googlecode.objectify.util.FutureNow;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

/**
 * The new datastore SDK has a neat structure of interfaces and implementations (transaction, datastorereader, etc)
 * but doesn't currently support async operations. We need to shim in a Future-based API so that we can seamlessly
 * support it when it becomes available. We'll remove this class then.
 */
@RequiredArgsConstructor
public class AsyncDatastoreReaderWriterImpl implements AsyncDatastoreReaderWriter {
	/** This is a hard limit imposed by the datastore (or the client sdk) */
	public static final int MAX_READ_SIZE = 1000;

	/** This is a hard limit imposed by the datastore (or the client sdk) */
	public static final int MAX_WRITE_SIZE = 500;

	private final DatastoreReaderWriter datastoreReaderWriter;

	@Override
	public Future<Map<Key, Entity>> get(final Collection<Key> keys, final ReadOption... options) {
		final Map<Key, Entity> result = new LinkedHashMap<>();

		final Iterable<List<Key>> partitions = Iterables.partition(keys, MAX_READ_SIZE);

		for (final List<Key> partition : partitions) {
			final Iterator<Entity> entities = (datastoreReaderWriter instanceof Datastore)
					? ((Datastore)datastoreReaderWriter).get(partition, options)
					: datastoreReaderWriter.get(Keys.toArray(partition));

			while (entities.hasNext()) {
				final Entity entity = entities.next();
				result.put(entity.getKey(), entity);
			}
		}

		return new FutureNow<>(result);
	}

	@Override
	public <T> QueryResults<T> run(final Query<T> query) {
		return datastoreReaderWriter.run(query);
	}

	@Override
	public Future<Void> delete(final Iterable<Key> keys) {
		final Iterable<List<Key>> partitions = Iterables.partition(keys, MAX_WRITE_SIZE);

		for (final List<Key> partition : partitions) {
			datastoreReaderWriter.delete(Iterables.toArray(partition, Key.class));
		}

		return new FutureNow<>(null);
	}

	@Override
	public Future<List<Key>> put(final Iterable<? extends FullEntity<?>> entities) {
		final Iterable<? extends List<? extends FullEntity<?>>> partitions = Iterables.partition(entities, MAX_WRITE_SIZE);

		final List<Key> result = new ArrayList<>();

		for (final List<? extends FullEntity<?>> partition : partitions) {
			final List<Entity> saved = datastoreReaderWriter.put(Iterables.toArray(partition, FullEntity.class));
			saved.stream().map(Entity::getKey).forEach(result::add);
		}

		return new FutureNow<>(result);
	}
}