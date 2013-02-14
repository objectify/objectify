/*
 * Copyright 2007 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.appengine.api.datastore;

import java.io.Serializable;
import java.util.Arrays;

/**
 * {@code Blob} contains an array of bytes.  This byte array can be no bigger
 * than 1MB.  To store files, particularly files larger than this 1MB limit,
 * look at the Blobstore API.
 *
 */
public final class Blob implements Serializable {

  private static final long serialVersionUID = 6210713401925622518L;

  private byte[] bytes;

  /**
   * Construct a new {@code Blob} with the specified bytes.  Since
   * {@code Blobs} can be quite large we do not perform a defensive copy of the
   * provided byte array.  It is the programmer's responsibility to avoid
   * making changes to this array once the {@code Blob} has been constructed.
   */
  public Blob(byte[] bytes) {
    this.bytes = bytes;
  }

  /**
   * This constructor exists for frameworks (e.g. Google Web Toolkit)
   * that require it for serialization purposes.  It should not be
   * called explicitly.
   */
  @SuppressWarnings("unused")
  private Blob(){
  }

  /**
   * Return the bytes stored in this {@code Blob}.
   */
  public byte[] getBytes() {
    return bytes;
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(bytes);
  }

  /**
   * Two {@code Blob} objects are considered equal if their contained
   * bytes match exactly.
   */
  @Override
  public boolean equals(Object object) {
    if (object instanceof Blob) {
      Blob key = (Blob) object;
      return Arrays.equals(bytes, key.bytes);
    }
    return false;
  }

  /**
   * Simply prints the number of bytes contained in this {@code Blob}.
   */
  @Override
  public String toString() {
    return "<Blob: " + bytes.length + " bytes>";
  }
}
