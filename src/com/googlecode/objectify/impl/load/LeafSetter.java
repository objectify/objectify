package com.googlecode.objectify.impl.load;


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.reflect.Array;
import java.util.Collection;

import com.google.appengine.api.datastore.Blob;
import com.google.appengine.api.datastore.Text;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.impl.LoadContext;
import com.googlecode.objectify.impl.TypeUtils;
import com.googlecode.objectify.impl.Wrapper;

/**
 * <p>Setter which knows how to set any kind of leaf value.  This could be any basic type
 * or a collection of basic types; basically anything except an @Embedded.</p>
 * 
 * <p>This class is a little weird.  It is a termination setter which itself cannot be
 * extended.  However, when constructed, it actually sets its next value to be a more-specific
 * type of setter defined by the three inner classes:  ForBasic, ForArray, and ForCollection.
 * This allows us to break up the logic a bit better.</p>
 * 
 * <p>This is always the termination of a setter chain; the {@code next} value is ignored.</p>
 */
public class LeafSetter extends CollisionDetectingSetter
{
	/** Knows how to set basic noncollection and nonarray values */
	class ForBasic extends Setter
	{
		@Override
		public void set(Object toPojo, Object value, LoadContext context)
		{
			field.set(toPojo, importBasic(value, field.getType()));
		}
	}
	
	/** Knows how to set arrays of basic types */
	class ForArray extends Setter
	{
		Class<?> componentType = field.getType().getComponentType();
		
		@Override
		public void set(Object toPojo, Object value, LoadContext context)
		{
			if (value == null)
			{
				field.set(toPojo, null);
			}
			else
			{
				if (!(value instanceof Collection<?>))
					throw new IllegalStateException("Cannot load non-collection value '" + value + "' into " + field);
	
				Collection<?> datastoreCollection = (Collection<?>)value;
	
				Object array = Array.newInstance(this.componentType, datastoreCollection.size());
	
				int index = 0;
				for (Object componentValue: datastoreCollection)
				{
					componentValue = importBasic(componentValue, this.componentType);
					Array.set(array, index++, componentValue);
				}
	
				field.set(toPojo, array);
			}
		}
	}

	/**
	 * <p>Deals with collections of basic types.</p>
	 * 
	 * <p>The special considerations for collections follows
	 * {@link TypeUtils#prepareCollection(Object, Wrapper)}</p>
	 */
	class ForCollection extends Setter
	{
		Class<?> componentType = TypeUtils.getComponentType(field.getType(), field.getGenericType());
		
		@Override
		public void set(Object toPojo, Object value, LoadContext context)
		{
			if (value == null)
			{
				field.set(toPojo, null);
			}
			else
			{
				if (!(value instanceof Collection<?>))
					throw new IllegalStateException("Cannot load non-collection value '" + value + "' into " + field);
	
				Collection<?> datastoreCollection = (Collection<?>)value;
				Collection<Object> target = TypeUtils.prepareCollection(toPojo, field, datastoreCollection.size());
	
				for (Object datastoreValue: datastoreCollection)
					target.add(importBasic(datastoreValue, componentType));
			}
		}
	}
	
	/** Need one of these to convert keys */
	ObjectifyFactory factory;
	
	/** The field or method we set */
	Wrapper field;
	
	/** If true, we expect a Blob and need to de-serialize it */
	boolean serialized;
	
	/** */
	public LeafSetter(ObjectifyFactory fact, Wrapper field, String collisionPath)
	{
		super(collisionPath);
		
		this.factory = fact;
		this.field = field;
		this.serialized = field.isSerialized();

		if (!this.serialized && field.getType().isArray())
		{
			this.next = new ForArray();
		}
		else if (!this.serialized && Collection.class.isAssignableFrom(field.getType()))
		{
			this.next = new ForCollection();
		}
		else
		{
			this.next = new ForBasic();
		}
	}
	
	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.load.CollisionDetectingSetter#safeSet(java.lang.Object, java.lang.Object, com.googlecode.objectify.impl.LoadContext)
	 */
	@Override
	protected void safeSet(Object obj, Object value, LoadContext context)
	{
		this.next.set(obj, value, context);
	}

	/**
	 * Converts a value obtained from the datastore into what gets sent on the field.
	 * The datastore translates values in ways that are not always convenient; for
	 * example, all numbers become Long and booleans become Boolean. This method translates
	 * just the basic types - not collection types.
	 *  
	 * @param fromValue	is the property value that came out of the datastore Entity
	 * @param toType	is the type to convert it to.
	 */
	@SuppressWarnings("unchecked")
	Object importBasic(Object fromValue, Class<?> toType)
	{
		if (fromValue == null)
		{
			return null;
		}
		else if (this.serialized)
		{
			// Above all others, if we're serialized, take the Blob and deserialize it.
			if (!(fromValue instanceof Blob))
				throw new IllegalStateException("Tried to deserialize non-Blob " + fromValue + " for field " + this.field);
			
			try
			{
				ByteArrayInputStream bais = new ByteArrayInputStream(((Blob)fromValue).getBytes());
				ObjectInputStream ois = new ObjectInputStream(bais);
				
				return ois.readObject();
			}
			catch (IOException ex) { throw new RuntimeException(ex); }
			catch (ClassNotFoundException ex) { throw new IllegalStateException("Unable to deserialize " + fromValue + " on field " + this.field + ": " + ex); }
		}
		else if (toType.isAssignableFrom(fromValue.getClass()))
		{
			return fromValue;
		}
		else if (toType == String.class)
		{
			if (fromValue instanceof Text)
				return ((Text) fromValue).getValue();
			else
				return fromValue.toString();
		}
		else if (Enum.class.isAssignableFrom(toType))
		{
			// Anyone have any idea how to avoid this generics warning?
			return Enum.valueOf((Class<Enum>) toType, fromValue.toString());
		}
		else if ((toType == Boolean.TYPE) && (fromValue instanceof Boolean))
		{
			return fromValue;
		}
		else if (fromValue instanceof Number)
		{
			return coerceNumber((Number) fromValue, toType);
		}
		else if (Key.class.isAssignableFrom(toType) && fromValue instanceof com.google.appengine.api.datastore.Key)
		{
			return this.factory.rawKeyToTypedKey((com.google.appengine.api.datastore.Key) fromValue);
		}

		throw new IllegalArgumentException("Don't know how to convert " + fromValue.getClass() + " to " + toType);
	}

	/**
	 * Coerces the value to be a number of the specified type; needed because
	 * all numbers come back from the datastore as Long and this screws up
	 * any type that expects something smaller.  Also does toString just for the
	 * hell of it.
	 */
	Object coerceNumber(Number value, Class<?> type)
	{
		if ((type == Byte.class) || (type == Byte.TYPE)) return value.byteValue();
		else if ((type == Short.class) || (type == Short.TYPE)) return value.shortValue();
		else if ((type == Integer.class) || (type == Integer.TYPE)) return value.intValue();
		else if ((type == Long.class) || (type == Long.TYPE)) return value.longValue();
		else if ((type == Float.class) || (type == Float.TYPE)) return value.floatValue();
		else if ((type == Double.class) || (type == Double.TYPE)) return value.doubleValue();
		else if (type == String.class) return value.toString();
		else throw new IllegalArgumentException("Don't know how to convert " + value.getClass() + " to " + type);
	}
	
	/** Ensure that nobody tries to extend the leaf nodes. */
	@Override
	final public Setter extend(Setter tail)
	{
		throw new UnsupportedOperationException("Objectify bug - cannot extend Leaf setters");
	}
}
