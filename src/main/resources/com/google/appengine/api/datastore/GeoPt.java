// Copyright 2008 Google Inc. All Rights Reserved.

package com.google.appengine.api.datastore;

import java.io.Serializable;

/**
 * A geographical point, specified by float latitude and longitude coordinates.
 * Often used to integrate with mapping sites like Google Maps.
 */
public final class GeoPt implements Serializable, Comparable<GeoPt> {

	public static final long serialVersionUID = 349808987517153697L;

	private float latitude;
	private float longitude;

	/**
	 * Constructs a {@code GeoPt}.
	 *
	 * @param latitude
	 *            The latitude. Must be between -90 and 90 (inclusive).
	 * @param longitude
	 *            The longitude. Must be between -180 and 180 (inclusive).
	 * @throws IllegalArgumentException
	 *             If {@code latitude} or {@code longitude} is outside the legal
	 *             range.
	 */
	public GeoPt(float latitude, float longitude) {
		if (Math.abs(latitude) > 90) {
			throw new IllegalArgumentException("Latitude must be between -90 and 90 (inclusive).");
		}

		if (Math.abs(longitude) > 180) {
			throw new IllegalArgumentException("Longitude must be between -180 and 180.");
		}

		this.latitude = latitude;
		this.longitude = longitude;
	}

	/**
	 * This constructor exists for frameworks (e.g. Google Web Toolkit) that
	 * require it for serialization purposes. It should not be called
	 * explicitly.
	 */
	@SuppressWarnings("unused")
	private GeoPt() {
		this(0, 0);
	}

	public float getLatitude() {
		return latitude;
	}

	public float getLongitude() {
		return longitude;
	}

	/**
	 * Sort first by latitude, then by longitude
	 */
	@Override
	public int compareTo(GeoPt o) {
		int latResult = ((Float) latitude).compareTo(o.latitude);
		if (latResult != 0) {
			return latResult;
		}
		return ((Float) longitude).compareTo(o.longitude);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		GeoPt geoPt = (GeoPt) o;

		if (Float.compare(geoPt.latitude, latitude) != 0) {
			return false;
		}
		if (Float.compare(geoPt.longitude, longitude) != 0) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result;
		result = (latitude != +0.0f ? Float.floatToIntBits(latitude) : 0);
		result = 31 * result + (longitude != +0.0f ? Float.floatToIntBits(longitude) : 0);
		return result;
	}

	@Override
	public String toString() {
		// String.format() is not part of GWT's JRE emulation
		//return String.format("%f,%f", latitude, longitude);
		return "GeoPt(" + latitude + ", " + longitude + ")";
	}
}
