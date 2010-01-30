package com.google.appengine.api.datastore;

import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * GWT emulation class. Will not have the same hashCode() value as the non-emulated version.
 * This class does not transmit the appId, and when deserializing on the server side, it just uses
 * the appId of the environment.
 *
 * Does not take applicationID of parents/children into consideration wrt Comparable, assumes they are both the same.
 */
public final class Key implements Serializable, Comparable<Key>
{
	static final long serialVersionUID = -448150158203091507L;
	
	private Key parentKey;
	private String kind;
	private long id;
	private String name;

	@SuppressWarnings("unused")
	private Key()
	{
		parentKey = null;
		kind = null;
		id = 0;
		name = null;
	}

	Key(String kind, String name)
	{
		this(kind, null, name);
	}

	Key(String kind, Key parentKey)
	{
		this(kind, parentKey, 0);
	}

	Key(String kind, Key parentKey, long id)
	{
		this(kind, parentKey, id, null);
	}

	Key(String kind, Key parentKey, String name)
	{
		this(kind, parentKey, 0, name);
	}

	Key(String kind, Key parentKey, long id, String name)
	{
		if (kind == null || kind.length() == 0)
			throw new IllegalArgumentException("No kind specified.");
		if (name != null)
		{
			if (name.length() == 0)
				throw new IllegalArgumentException("Name may not be empty.");
			if (id != 0)
				throw new IllegalArgumentException("Id and name may not both be specified at once.");
		}
		this.name = name;
		this.id = id;
		this.parentKey = parentKey;
		this.kind = kind;
	}

	public String getKind()
	{
		return kind;
	}

	public Key getParent()
	{
		return parentKey;
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		Key key = (Key) o;

		if (id != key.id) return false;
		if (!kind.equals(key.kind)) return false;
		if (name != null ? !name.equals(key.name) : key.name != null) return false;
		if (parentKey != null ? !parentKey.equals(key.parentKey) : key.parentKey != null) return false;

		return true;
	}

	@Override
	public int hashCode()
	{
		int result = parentKey != null ? parentKey.hashCode() : 0;
		result = 31 * result + kind.hashCode();
		result = 31 * result + (int) (id ^ (id >>> 32));
		result = 31 * result + (name != null ? name.hashCode() : 0);
		return result;
	}

	public String toString()
	{
		StringBuilder buffer = new StringBuilder();
		appendToString(buffer);
		return buffer.toString();
	}

	private void appendToString(StringBuilder buffer)
	{
		if (parentKey != null)
		{
			parentKey.appendToString(buffer);
			buffer.append("/");
		}
		buffer.append(kind);
		buffer.append("(");
		if (name != null)
			buffer.append('"').append(name).append('"');
		else
			buffer.append(id);
		buffer.append(")");
	}

	public long getId()
	{
		return id;
	}

	public String getName()
	{
		return name;
	}

	public Key getChild(String kind, long id)
	{
		return new Key(kind, this, id);
	}

	public Key getChild(String kind, String name)
	{
		return new Key(kind, this, name);
	}

	private static Iterator<Key> getPathIterator(Key key)
	{
		LinkedList<Key> stack = new LinkedList<Key>();
		do
		{
			stack.addFirst(key);
			key = key.getParent();
		} while (key != null);

		return stack.iterator();
	}

	public int compareTo(Key other)
	{
		if (this == other)
			return 0;
		Iterator<Key> thisPath = getPathIterator(this);
		Iterator<Key> otherPath = getPathIterator(other);
		while (thisPath.hasNext())
		{
			Key thisKey = (Key) thisPath.next();
			if (otherPath.hasNext())
			{
				Key otherKey = (Key) otherPath.next();
				int result = compareToInternal(thisKey, otherKey);
				if (result != 0)
					return result;
			}
			else
			{
				return 1;
			}
		}
		return otherPath.hasNext() ? -1 : 0;
	}

	private static int compareToInternal(Key thisKey, Key otherKey)
	{
		if (thisKey == otherKey)
			return 0;
		int result = thisKey.getKind().compareTo(otherKey.getKind());
		if (result != 0)
			return result;
		if (thisKey.getId() != 0)
			if (otherKey.getId() == 0)
				return -1;
			else
				return Long.valueOf(thisKey.getId()).compareTo(Long.valueOf(otherKey.getId()));
		if (otherKey.getId() != 0)
			return 1;
		else
			return thisKey.getName().compareTo(otherKey.getName());
	}
}

