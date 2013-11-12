package com.googlecode.objectify;

import com.google.appengine.api.datastore.EmbeddedEntity;

/**
 * Interface to allow serialization/deserialization of objects state to/from EmbeddedEntity
 *
 * @author Huseyn Guliyev<husayt@gmail.com>
 */
public interface IEmbeddedEntity<T extends IEmbeddedEntity<T>> {
    /**
     * Creates an instance of embedded entity to be stored in datastore from this
     */
    EmbeddedEntity createEmbeddedEntity();

    /**
     * set fields for this from embedded entity that was read from datastore
     */
    T setFieldsFrom(EmbeddedEntity embeddedEntity);
}
