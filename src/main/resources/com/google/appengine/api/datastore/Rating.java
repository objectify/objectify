// Copyright 2009 Google Inc. All Rights Reserved.
package com.google.appengine.api.datastore;

import java.io.Serializable;

/**
 * A user-provided integer rating for a piece of content. Normalized to a 0-100
 * scale.
 *
 */
public final class Rating implements Serializable, Comparable<Rating> {

	public static final long serialVersionUID = 362898405551261187L;

	/**
	 * The minimum legal value for a rating.
	 */
	public static final int MIN_VALUE = 0;

	/**
	 * The maximum legal value for a rating.
	 */
	public static final int MAX_VALUE = 100;

	private int rating;

	/**
	 * @throws IllegalArgumentException
	 *             If {@code rating} is smaller than {@link #MIN_VALUE} or
	 *             greater than {@link #MAX_VALUE}
	 */
	public Rating(int rating) {
		if (rating < MIN_VALUE || rating > MAX_VALUE) {
// String.format() not part of GWT JRE emulation
//			throw new IllegalArgumentException(String.format(
//					"rating must be no smaller than %d and no greater than %d (received %d)", MIN_VALUE, MAX_VALUE,
//					rating));
			throw new IllegalArgumentException(
					"rating must be no smaller than " + MIN_VALUE + " and no greater than " + MAX_VALUE + " (received " + rating + ")");
		}
		this.rating = rating;
	}

	/**
	 * This constructor exists for frameworks (e.g. Google Web Toolkit) that
	 * require it for serialization purposes. It should not be called
	 * explicitly.
	 */
	@SuppressWarnings("unused")
	private Rating() {
		this(0);
	}

	public int getRating() {
		return rating;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		Rating rating1 = (Rating) o;

		if (rating != rating1.rating) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		return rating;
	}

	@Override
	public int compareTo(Rating o) {
		return Integer.valueOf(rating).compareTo(o.rating);
	}
}
