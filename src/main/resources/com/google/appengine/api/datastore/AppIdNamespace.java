// Copyright 2009 Google Inc. All Rights Reserved.

package com.google.appengine.api.datastore;

import com.google.apphosting.api.NamespaceResources;

import java.io.Serializable;

/**
 * Abstraction for a "mangled" AppId.  A mangled AppId is a combination
 * of the application id and the name_space which this class will
 * manage.
 *
 */
class AppIdNamespace implements Serializable, Comparable<AppIdNamespace> {
  private final String appId;
  private final String namespace;

  private static final String BAD_APP_ID_MESSAGE =
      "appId or namespace cannot contain '" + NamespaceResources.NAMESPACE_SEPARATOR + "'";

  /**
   * Constructs an {@link AppIdNamespace} given {@code #appId} and {@code #namespace}.
   */
  public AppIdNamespace(String appId, String namespace) {
    if (appId == null || namespace == null) {
      throw new IllegalArgumentException("appId or namespace may not be null");
    }
    if (appId.indexOf(NamespaceResources.NAMESPACE_SEPARATOR) != -1 ||
        namespace.indexOf(NamespaceResources.NAMESPACE_SEPARATOR) != -1) {
      throw new IllegalArgumentException(BAD_APP_ID_MESSAGE);
    }
    this.appId = appId;
    this.namespace = namespace;
  }

  /**
   * Converts an encoded appId/namespace to {@link AppIdNamespace}.
   *
   * <p>Only one form of an appId/namespace pair will be allowed. i.e. "app!"
   * is an illegal form and must be encoded as "app".
   *
   * <p>An appId/namespace pair may contain at most one "!" character.
   *
   * @param encodedAppIdNamespace The encoded application Id/namespace string.
   */
  public static AppIdNamespace parseEncodedAppIdNamespace(String encodedAppIdNamespace) {
    if (encodedAppIdNamespace == null) {
      throw new IllegalArgumentException("appIdNamespaceString may not be null");
    }
    int index = encodedAppIdNamespace.indexOf(NamespaceResources.NAMESPACE_SEPARATOR);
    if (index == -1) {
      return new AppIdNamespace(encodedAppIdNamespace, "");
    }
    String appId = encodedAppIdNamespace.substring(0, index);
    String namespace = encodedAppIdNamespace.substring(index + 1);
    if (namespace.length() == 0) {
      throw new IllegalArgumentException(
          "encodedAppIdNamespace with empty namespace may not contain a '" +
          NamespaceResources.NAMESPACE_SEPARATOR + "'");
    }
    return new AppIdNamespace(appId, namespace);
  }

  /**
   * Perform a "lexical" comparison to {@code other} {@link AppIdNamespace}.
   * @return See {@link String#compareTo(String)}.
   */
  @Override
  public int compareTo(AppIdNamespace other) {
    int appidCompare = appId.compareTo(other.appId);
    if (appidCompare == 0) {
      return namespace.compareTo(other.namespace);
    }
    return appidCompare;
  }

  public String getAppId() {
    return appId;
  }

  public String getNamespace() {
    return namespace;
  }

  /**
   * Returns an "encoded" appId/namespace string.
   *
   * <p>Note: If the {@link #namespace} is empty, the return value is exactly the {@link #appId}.
   */
  public String toEncodedString() {
    if (namespace.equals("")) {
      return appId;
    } else {
      return appId + NamespaceResources.NAMESPACE_SEPARATOR + namespace;
    }
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((appId == null) ? 0 : appId.hashCode());
    result = prime * result + ((namespace == null) ? 0 : namespace.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    AppIdNamespace other = (AppIdNamespace) obj;
    if (appId == null) {
      if (other.appId != null) {
        return false;
      }
    } else if (!appId.equals(other.appId)) {
      return false;
    }
    if (namespace == null) {
      if (other.namespace != null) {
        return false;
      }
    } else if (!namespace.equals(other.namespace)) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return toEncodedString();
  }
}
