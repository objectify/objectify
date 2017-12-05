package com.googlecode.objectify.impl;

import com.google.cloud.datastore.DatastoreReaderWriter;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.FullEntity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.QueryResults;
import com.google.common.collect.Iterables;
import com.googlecode.objectify.util.FutureNow;
import lombok.RequiredArgsConstructor;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

/**
 * The new datastore SDK has a neat structure of interfaces and implementations (transaction, datastorereader, etc)
 * but doesn't currently support async operations. We need to shim in a Future-based API so that we can seamlessly
 * support it when it becomes available. We'll remove this class then.
 */
@RequiredArgsConstructor
public class AsyncDatastoreReaderWriterImpl implements AsyncDatastoreReaderWriter {
	private final DatastoreReaderWriter datastoreReaderWriter;

	@Override
	public Future<Map<Key, Entity>> get(final Key... keys) {
		final Iterator<Entity> entities = datastoreReaderWriter.get(keys);

		final Map<Key, Entity> map = new LinkedHashMap<>();
		while (entities.hasNext()) {
			final Entity entity = entities.next();
			map.put(entity.getKey(), entity);
		}

		return new FutureNow<>(map);
	}

	@Override
	public <T> QueryResults<T> run(final Query<T> query) {
		return datastoreReaderWriter.run(query);
	}

	@Override
	public Future<Void> delete(final Iterable<Key> keys) {
		datastoreReaderWriter.delete(Iterables.toArray(keys, Key.class));
		return new FutureNow<>(null);
	}

	@Override
	public Future<List<Key>> put(final Iterable<? extends FullEntity<?>> entities) {
		final List<Entity> saved = datastoreReaderWriter.put(Iterables.toArray(entities, FullEntity.class));
		final List<Key> keys = saved.stream().map(Entity::getKey).collect(Collectors.toList());
		return new FutureNow<>(keys);
	}
}