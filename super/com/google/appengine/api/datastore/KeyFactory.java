package com.google.appengine.api.datastore;

/**
 * GWT emulation class, with most of the methods from KeyFactory.
 * Does not support the keyToString()/stringToKey() methods.
 */
public class KeyFactory
{
	public static final class Builder
	{
		public Builder addChild(String kind, String name)
		{
			current = KeyFactory.createKey(current, kind, name);
			return this;
		}

		public Builder addChild(String kind, long id)
		{
			current = KeyFactory.createKey(current, kind, id);
			return this;
		}

		public Key getKey()
		{
			return current;
		}

		private Key current;

		public Builder(String kind, String name)
		{
			current = KeyFactory.createKey(null, kind, name);
		}

		public Builder(String kind, long id)
		{
			current = KeyFactory.createKey(null, kind, id);
		}

		public Builder(Key key)
		{
			current = key;
		}
	}

	public static Key createKey(String kind, long id)
	{
		return createKey(null, kind, id);
	}

	public static Key createKey(Key parent, String kind, long id)
	{
		if (id == 0L)
			throw new IllegalArgumentException("id cannot be zero");
		else
			return new Key(kind, parent, id);
	}

	public static Key createKey(String kind, String name)
	{
		return createKey(null, kind, name);
	}

	public static Key createKey(Key parent, String kind, String name)
	{
		if (name == null || name.length() == 0)
			throw new IllegalArgumentException("name cannot be null or empty");
		else
			return new Key(kind, parent, name);
	}

	private KeyFactory()
	{
	}

}
