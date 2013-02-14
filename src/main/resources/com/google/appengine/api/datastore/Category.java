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
 * A tag, ie a descriptive word or phrase. Entities may be tagged by users,
 * and later returned by a queries for that tag. Tags can also be used for
 * ranking results (frequency), photo captions, clustering, activity, etc.
 *
 * @see <a href="http://www.zeldman.com/daily/0405d.shtml">Jeffrey Zeldmans blog post</a>
 * on tag clouds for a more in-depth description.
 */
public final class Category implements Serializable, Comparable<Category> {

  public static final long serialVersionUID = 8556134984576082397L;

  private String category;

  public Category(String category) {
    if (category == null) {
      throw new NullPointerException("category must not be null");
    }
    this.category = category;
  }

  /**
   * This constructor exists for frameworks (e.g. Google Web Toolkit)
   * that require it for serialization purposes.  It should not be
   * called explicitly.
   */
  @SuppressWarnings("unused")
  private Category() {
    this.category = null;
  }

  public String getCategory() {
    return category;
  }

  @Override
  public int compareTo(Category o) {
    return category.compareTo(o.category);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Category category1 = (Category) o;

    if (!category.equals(category1.category)) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    return category.hashCode();
  }
}
