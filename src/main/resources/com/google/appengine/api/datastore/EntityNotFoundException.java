// Copyright 2007 Google Inc. All rights reserved.

package com.google.appengine.api.datastore;

/**
 * {@code EntityNotFoundException} is thrown when no {@code Entity}
 * with the specified {@code Key} could be found.
 *
 */
public class EntityNotFoundException extends Exception {
  private final Key key;

  public EntityNotFoundException(Key key) {
    super("No entity was found matching the key: " + key);
    this.key = key;
  }

  public Key getKey() {
    return key;
  }
}
