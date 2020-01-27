package com.googlecode.objectify.impl;

import com.google.cloud.datastore.BaseKey;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.FullEntity;
import com.google.cloud.datastore.IncompleteKey;
import com.google.cloud.datastore.KeyFactory;
import com.google.cloud.datastore.LongValue;
import com.google.cloud.datastore.StringValue;
import com.google.cloud.datastore.Value;
import com.google.common.base.Preconditions;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.NamespaceManager;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.util.KeyFormat;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;


/**
 * <p>Gives us a slightly more organized interface for manipulating keys. While this is part of Objectify's
 * public interface, you probably shouldn't use it. It's subject to change without notice. If you want to
 * work with keys, use the Key.create() methods.</p>
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
@RequiredArgsConstructor
public class Keys
{
	private final Datastore datastore;
	private final Registrar registrar;

	/**
	 * @return the Key<?> for a registered pojo entity.
	 */
	public <T> Key<T> keyOf(final T pojo, final String namespaceHint) {
		return Key.create(rawKeyOf(pojo, namespaceHint));
	}

	/**
	 * @return the native datastore key for a registered pojo entity (or FullEntity).
	 */
	public com.google.cloud.datastore.Key rawKeyOf(final Object pojo, final String namespaceHint) {
		if (pojo instanceof FullEntity<?>) {
			return (com.google.cloud.datastore.Key)((FullEntity)pojo).getKey();
		} else {
			return getMetadataSafe(pojo).getCompleteKey(pojo, namespaceHint);
		}
	}

	/**
	 * @return the metadata for a registered pojo, or throw exception if none
	 * @throws IllegalStateException if the pojo class has not been registered
	 */
	public <T> KeyMetadata<T> getMetadataSafe(final Class<T> clazz) {
		return registrar.getMetadataSafe(clazz).getKeyMetadata();
	}

	/**
	 * @return the metadata for a registeerd pojo, or throw exception if none
	 * @throws IllegalStateException if the pojo class has not been registered
	 */
	@SuppressWarnings("unchecked")
	public <T> KeyMetadata<T> getMetadataSafe(final T pojo) {
		return (KeyMetadata<T>)getMetadataSafe(pojo.getClass());
	}

	/**
	 * @return the metadata for a registered pojo, or null if there is none
	 */
	@SuppressWarnings("unchecked")
	public <T> KeyMetadata<T> getMetadata(final Key<T> key) {
		final EntityMetadata<T> em = registrar.getMetadata(key.getKind());
		return em == null ? null : em.getKeyMetadata();
	}

	/**
	 * <p>Gets the Key<T> given an object that might be a Key, Key<T>, or entity.</p>
	 *
	 * @param keyOrEntity must be a Key, Key<T>, or registered entity.
	 * @throws NullPointerException if keyOrEntity is null
	 * @throws IllegalArgumentException if keyOrEntity is not a Key, Key<T>, or registered entity
	 */
	@SuppressWarnings("unchecked")
	public <T> Key<T> anythingToKey(final Object keyOrEntity, final String namespaceHint) {

		if (keyOrEntity instanceof Key<?>)
			return (Key<T>)keyOrEntity;
		else if (keyOrEntity instanceof com.google.cloud.datastore.Key)
			return Key.create((com.google.cloud.datastore.Key)keyOrEntity);
		else if (keyOrEntity instanceof Ref)
			return ((Ref<T>)keyOrEntity).key();
		else if (keyOrEntity instanceof FullEntity<?>)
			return Key.create(getKey((FullEntity<?>)keyOrEntity));
		else
			return keyOf((T)keyOrEntity, namespaceHint);
	}

	/**
	 * <p>Gets the raw datstore Key given an object that might be a Key, Key<T>, or entity.</p>
	 *
	 * @param keyOrEntity must be a Key, Key<T>, or registered entity.
	 * @throws NullPointerException if keyOrEntity is null
	 * @throws IllegalArgumentException if keyOrEntity is not a Key, Key<T>, or registered entity
	 */
	public com.google.cloud.datastore.Key anythingToRawKey(final Object keyOrEntity, final String namespaceHint) {

		if (keyOrEntity instanceof com.google.cloud.datastore.Key)
			return (com.google.cloud.datastore.Key)keyOrEntity;
		else if (keyOrEntity instanceof Key<?>)
			return ((Key<?>)keyOrEntity).getRaw();
		else if (keyOrEntity instanceof Ref)
			return ((Ref<?>)keyOrEntity).key().getRaw();
		else if (keyOrEntity instanceof FullEntity<?>)
			return getKey((FullEntity<?>)keyOrEntity);
		else
			return rawKeyOf(keyOrEntity, namespaceHint);
	}

	/** @return the Key, or throw an exception if entity's key is missing or incomplete */
	public static com.google.cloud.datastore.Key getKey(final FullEntity<?> entity) {
		Preconditions.checkNotNull(entity.getKey(), "FullEntity<?> is expected to have a key");
		Preconditions.checkArgument(entity.getKey() instanceof com.google.cloud.datastore.Key, "Unexpected IncompleteKey");
		return (com.google.cloud.datastore.Key)entity.getKey();
	}

	/**
	 * @return true of the entity has a null id which can be autogenerated on save
	 */
	public boolean requiresAutogeneratedId(final Object entity) {
		if (entity instanceof FullEntity<?>)
			return !(((FullEntity)entity).getKey() instanceof com.google.cloud.datastore.Key);
		else
			return getMetadataSafe(entity).requiresAutogeneratedId(entity);
	}

	/** Also checks the inferred namespace */
	private void checkNamespaceAndParent(final String namespace, final com.google.cloud.datastore.Key parent) {
		if (parent != null) {
			if (namespace != null && !namespace.equals(parent.getNamespace()))
				throw new IllegalStateException("You cannot specify a namespace and a parent key from a different namespace. Explicit namespace is '" + namespace + "', parent namespace is '" + parent.getNamespace() + "'.");

			final String ns = NamespaceManager.get();
			if (ns != null && !ns.equals(parent.getNamespace()))
				throw new IllegalStateException("You cannot specify a namespace via the NamespaceManager and a parent key from a different namespace. NamespaceManager namespace is '" + ns + "', parent namespace is '" + parent.getNamespace() + "'.");
		}
	}

	/**
	 * namespace and parent are mutually exclusive
	 * @param parent can be null for root keys
	 */
	public com.google.cloud.datastore.Key createRaw(final String namespace, final com.google.cloud.datastore.Key parent, final String kind, final long id) {
		if (parent == null) {
			return adjustNamespace(datastore.newKeyFactory().setKind(kind), namespace).newKey(id);
		} else {
			checkNamespaceAndParent(namespace, parent);
			return com.google.cloud.datastore.Key.newBuilder(parent, kind, id).build();
		}
	}

	/**
	 * namespace and parent are mutually exclusive
	 * @param parent can be null for root keys
	 */
	public com.google.cloud.datastore.Key createRaw(final String namespace, final com.google.cloud.datastore.Key parent, final String kind, final String name) {
		if (parent == null) {
			return adjustNamespace(datastore.newKeyFactory().setKind(kind), namespace).newKey(name);
		} else {
			checkNamespaceAndParent(namespace, parent);
			return com.google.cloud.datastore.Key.newBuilder(parent, kind, name).build();
		}
	}

	/**
	 * Construct a Key from a Long or String id
	 * @param id must be either Long or String
	 */
	public com.google.cloud.datastore.Key createRawAny(final String namespace, final com.google.cloud.datastore.Key parent, final String kind, final Object id) {
		if (id instanceof String)
			return createRaw(namespace, parent, kind, (String)id);
		else if (id instanceof Long)
			return createRaw(namespace, parent, kind, (Long)id);
		else
			throw new IllegalArgumentException("id '" + id + "' must be String or Long");
	}

	/**
	 * namespace and parent are mutually exclusive
	 * @param parent can be null for root keys
	 */
	public IncompleteKey createRawIncomplete(final String namespace, final com.google.cloud.datastore.Key parent, final String kind) {
		if (parent == null) {
			return adjustNamespace(datastore.newKeyFactory().setKind(kind), namespace).newKey();
		} else {
			checkNamespaceAndParent(namespace, parent);
			return IncompleteKey.newBuilder(parent, kind).build();
		}
	}

	/**
	 * namespace and parent are mutually exclusive
	 * @param parent can be null for root keys
	 */
	public <T> Key<T> createKey(final String namespace, final Key<?> parent, final Class<T> kind, final long id) {
		final com.google.cloud.datastore.Key key = createRaw(namespace, raw(parent), Key.getKind(kind), id);
		return Key.create(key);
	}

	/**
	 * namespace and parent are mutually exclusive
	 * @param parent can be null for root keys
	 */
	public <T> Key<T> createKey(final String namespace, final Key<?> parent, final Class<T> kind, final String name) {
		final com.google.cloud.datastore.Key key = createRaw(namespace, raw(parent), Key.getKind(kind), name);
		return Key.create(key);
	}

	/**
	 * Construct a Key<?> from a Long or String id
	 * namespace and parent are mutually exclusive
	 *
	 * @param id must be either Long or String
	 */
	public <T> Key<T> createKeyAny(final String namespace, final Key<?> parent, final Class<T> kind, final Object id) {
		if (id instanceof String)
			return createKey(namespace, parent, kind, (String)id);
		else if (id instanceof Long)
			return createKey(namespace, parent, kind, (Long)id);
		else
			throw new IllegalArgumentException("id '" + id + "' must be String or Long");
	}

	/** Null-safe extraction of the raw key */
	public static com.google.cloud.datastore.Key raw(final Key<?> key) {
		return key == null ? null : key.getRaw();
	}

	/**
	 * Make a list of Key<?>s
	 * @param namespace must be exclusive with parent; only one can be provided
	 * @param parent must be exclusive with namespace; only one can be provided
	 * @param ids must contain either Long or String
	 */
	public <T> List<Key<T>> createKeys(final String namespace, final Key<?> parent, final Class<T> kind, final Iterable<?> ids) {
		return StreamSupport.stream(ids.spliterator(), false)
				.map(id -> createKeyAny(namespace, parent, kind, id))
				.collect(Collectors.toList());
	}

	public static com.google.cloud.datastore.Key[] toArray(final Collection<com.google.cloud.datastore.Key> collection) {
		return collection.toArray(new com.google.cloud.datastore.Key[collection.size()]);
	}

	/**
	 * Gets the String or Long id from the key as a Value, or null if incomplete
	 */
	@SuppressWarnings("unchecked")
	public static <S> Value<S> getIdValue(final IncompleteKey key) {
		if (key instanceof com.google.cloud.datastore.Key) {
			final com.google.cloud.datastore.Key completeKey = (com.google.cloud.datastore.Key)key;
			if (completeKey.hasId())
				return (Value<S>)LongValue.of(completeKey.getId());
			else
				return (Value<S>)StringValue.of(completeKey.getName());
		} else {
			return null;
		}
	}

	/**
	 * Understands both the legacy format "ag1zfnZvb2Rvb2R5bmUwcgcLEgFCGAEM" and new format,
	 * providing the key either way.
	 */
	@SneakyThrows
	public static com.google.cloud.datastore.Key fromUrlSafe(final String urlSafeKey) {
		if (urlSafeKey.startsWith("a")) {
			return KeyFormat.INSTANCE.parseOldStyleAppEngineKey(urlSafeKey);
		} else {
			return com.google.cloud.datastore.Key.fromUrlSafe(urlSafeKey);
		}
	}

	/** Be sensitive to the thread local namespace, if set */
	public static KeyFactory adjustNamespace(final KeyFactory keyFactory, final String namespace) {
		final String ns = namespace != null ? namespace : NamespaceManager.get();

		if (ns != null)
			keyFactory.setNamespace(ns);

		return keyFactory;
	}

	/** Be sensitive to the thread local namespace, if set */
	public static <B extends BaseKey.Builder<B>> B adjustNamespace(final B builder, final String namespace) {
		final String ns = namespace != null ? namespace : NamespaceManager.get();

		if (ns != null)
			builder.setNamespace(ns);

		return builder;
	}
}
