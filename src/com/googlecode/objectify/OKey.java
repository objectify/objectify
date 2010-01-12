package com.googlecode.objectify;

import java.io.Serializable;

/**
 * <p>This is a typesafe version of the Key object.  It is also Serializable
 * and GWT-safe, enabling your entity objects to be used for GWT RPC should
 * you so desire.</p>
 * 
 * <p>You may use normal Key objects as relationships in your entities if you
 * desire neither type safety nor GWTability.</p>
 * 
 * <p>Note that this class is deliberately not final and can be extended
 * easily.  We recommend you derive a new OFactory and override the createKey()
 * methods if you choose to go this route.</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 * @author Scott Hernandez
 */
public class OKey<T> implements Serializable, Comparable<OKey<?>>
{
	private static final long serialVersionUID = 1L;
	
	/** 
	 * The name of the class which represents the kind.  As much as
	 * we'd like to use the normal String kind value here, translating
	 * back to a Class for getKind() would then require a link to the
	 * OFactory, making this object non-serializable.
	 */
	protected Class<T> kindClass;
	
	/** Null if there is no parent */
	protected OKey<?> parent;
	
	/** Either id or name will be valid */
	protected long id;

	/** Either id or name will be valid */
	protected String name;
	
	/** Construct a key with a long id */
	protected OKey(OKey<?> parent, Class<T> kind, long id)
	{
		this.parent = parent;
		this.kindClass = kind;
		this.id = id;
	}
	
	/** Construct a key with a String name */
	protected OKey(OKey<?> parent, Class<T> kind, String name)
	{
		this.parent = parent;
		this.kindClass = kind;
		this.name = name;
	}

	/**
	 * @return the id associated with this key, or 0 if this key has a name.
	 */
	public long getId()
	{
		return this.id;
	}
	
	/**
	 * @return the name associated with this key, or null if this key has an id
	 */
	public String getName()
	{
		return this.name;
	}
	
	/**
	 * @return the Class associated with this key.
	 */
	public Class<?> getKind()
	{
		return this.kindClass;
	}
	
	/**
	 * @return the parent key, or null if there is no parent.  Note that
	 *  the parent could potentially have any type. 
	 */
	@SuppressWarnings("unchecked")
	public <V> OKey<V> getParent()
	{
		return (OKey<V>)this.parent;
	}

	/**
	 * <p>Compares based on the following traits, in order:</p>
	 * <ol>
	 * <li>kind</li>
	 * <li>parent</li>
	 * <li>id or name</li>
	 * </ol>
	 */
	@Override
	public int compareTo(OKey<?> other)
	{
		// First kind
		int cmp = this.kindClass.getName().compareTo(other.kindClass.getName());
		if (cmp != 0)
			return cmp;

		// Then parent
		cmp = compareNullable(this.parent, other.parent);
		if (cmp != 0)
			return cmp;
		
		// Then either id or name, whichever exists - but they might be different
		cmp = compareNullable(this.name, other.name);
		if (cmp != 0)
			return cmp;

		if (this.id < other.id)
			return -1;
		else if (this.id > other.id)
			return 1;
		else
			return 0;
	}

	/** */
	@Override
	public boolean equals(Object obj)
	{
		if (obj == null)
			return false;
		
		if (!(obj instanceof OKey<?>))
			return false;
		
		return this.compareTo((OKey<?>)obj) == 0;
	}

	/** */
	@Override
	public int hashCode()
	{
		if (this.name != null)
			return this.name.hashCode();
		else
			return (int)this.id;
	}

	/** Creates a human-readable version of this key */
	@Override
	public String toString()
	{
		StringBuilder bld = new StringBuilder();
		bld.append("OKey{kind=");
		bld.append(this.kindClass.getName());
		bld.append(", parent=");
		bld.append(this.parent);
		if (this.name != null)
		{
			bld.append(", name=");
			bld.append(this.name);
		}
		else
		{
			bld.append(", id=");
			bld.append(this.id);
		}
		bld.append("}");
		
		return bld.toString();
	}
	
	/** */
	@SuppressWarnings("unchecked")
	private static int compareNullable(Comparable o1, Comparable o2)
	{
		if (o1 == null && o2 == null)
			return 0;
		if (o1 == null && o2 != null)
			return -1;
		else if (o1 != null && o2 == null)
			return 1;
		else
			return o1.compareTo(o2);
	}
}