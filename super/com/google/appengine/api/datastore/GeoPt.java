package com.google.appengine.api.datastore;

import java.io.Serializable;

/**
 * GWT emulation class.
 */
@SuppressWarnings("serial")
public class GeoPt implements Serializable, Comparable<GeoPt>
{
	private final float latitude;
	private final float longitude;

	public GeoPt(float latitude, float longitude)
	{
		if (Math.abs(latitude) > 90F)
			throw new IllegalArgumentException("Latitude must be between -90 and 90 (inclusive).");
		if (Math.abs(longitude) > 180F)
		{
			throw new IllegalArgumentException("Latitude must be between -180 and 180.");
		}
		else
		{
			this.latitude = latitude;
			this.longitude = longitude;
		}
	}

	@SuppressWarnings("unused")
	private GeoPt()
	{
		this(0.0F, 0.0F);
	}

	public float getLatitude()
	{
		return latitude;
	}

	public float getLongitude()
	{
		return longitude;
	}

	public int compareTo(GeoPt o)
	{
		int latResult = Float.valueOf(latitude).compareTo(Float.valueOf(o.latitude));
		if (latResult != 0)
			return latResult;
		else
			return Float.valueOf(longitude).compareTo(Float.valueOf(o.longitude));
	}

	public boolean equals(Object o)
	{
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		GeoPt geoPt = (GeoPt) o;
		if (Float.compare(geoPt.latitude, latitude) != 0)
			return false;
		return Float.compare(geoPt.longitude, longitude) == 0;
	}


	public int hashCode()
	{
		int result = (int) latitude;
		result = 31 * result + ((int) longitude);
		return result;
	}

	public String toString()
	{
		return latitude + "," + longitude;
	}
}
