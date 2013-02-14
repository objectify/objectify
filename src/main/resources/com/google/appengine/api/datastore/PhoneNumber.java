/*
 * Copyright 2009 Google Inc.
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

/**
 * A human-readable phone number.  No validation is performed because phone
 * numbers have many different formats - local, long distance, domestic,
 * international, internal extension, TTY, VOIP, SMS, and alternative networks
 * like Skype, XFire and Roger Wilco.  They all have their own numbering and
 * addressing formats.
 *
 */
public final class PhoneNumber implements Serializable, Comparable<PhoneNumber> {

  public static final long serialVersionUID = -8968032543663409348L;

  private String number;

  public PhoneNumber(String number) {
    if (number == null) {
      throw new NullPointerException("number must not be null");
    }
    this.number = number;
  }

  /**
   * This constructor exists for frameworks (e.g. Google Web Toolkit)
   * that require it for serialization purposes.  It should not be
   * called explicitly.
   */
  @SuppressWarnings("unused")
  private PhoneNumber() {
    number = null;
  }

  public String getNumber() {
    return number;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    PhoneNumber that = (PhoneNumber) o;

    if (!number.equals(that.number)) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    return number.hashCode();
  }

  @Override
  public int compareTo(PhoneNumber o) {
    return number.compareTo(o.number);
  }
}
