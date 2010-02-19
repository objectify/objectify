package com.googlecode.objectify.impl.save;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;

import com.google.appengine.api.datastore.Blob;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Text;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.annotation.Serialized;
import com.googlecode.objectify.impl.TypeUtils;

/**
 * <p>Saver which knows how to save basic leaf values. Leaf values are things that
 * go into the datastore: basic types or collections of basic types.  Basically
 * anything except an @Embedded.</p>
 */
public class LeafFieldSaver extends FieldSaver
{
	/** We need this to translate keys */
	ObjectifyFactory factory;
	
	/** If true, we add values to a collection inside the entity */
	boolean collectionize;
	
	/** If true, we serialize the value into a Blob */
	boolean serialize;
	
	/** If true, null values are not saved. Leaf collection types are treated this way. */
	boolean ignoreIfNull;
	
	/**
	 * @param field must be a noncollection, nonarray type if collectionize is true
	 * @param collectionize when true will cause this leaf saver to persist simple basic
	 *  types in a collection inside the entity property.  If set is called multiple times,
	 *  the collection will be appended to. 
	 */
	public LeafFieldSaver(ObjectifyFactory fact, String pathPrefix, Field field, boolean inheritedIndexed, boolean collectionize)
	{
		super(pathPrefix, field, inheritedIndexed);
		
		this.factory = fact;
		this.collectionize = collectionize;
		this.serialize = field.isAnnotationPresent(Serialized.class);
		
		if (this.collectionize)
			if (!this.serialize && TypeUtils.isArrayOrCollection(field.getType()))
				throw new IllegalStateException("Cannot place array or collection properties inside @Embedded arrays or collections. The offending field is " + field);
		
		// Don't save null arrays or collections
		if (!this.serialize && TypeUtils.isArrayOrCollection(field.getType()))
			this.ignoreIfNull = true;
	}
	
	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.Saver#save(java.lang.Object, com.google.appengine.api.datastore.Entity)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public void save(Object pojo, Entity entity)
	{
		Object value = TypeUtils.field_get(this.field, pojo);
		value = this.prepareForSave(value);
		
		// Maybe we are supposed to ignore this
		if (value == null && this.ignoreIfNull)
			return;
		
		if (this.collectionize)
		{
			Collection<Object> savedCollection = (Collection<Object>)entity.getProperty(this.path);
			if (savedCollection == null)
			{
				savedCollection = new ArrayList<Object>();
				this.setEntityProperty(entity, savedCollection);
			}
			
			savedCollection.add(value);
		}
		else
		{
			if (value instanceof Collection && ((Collection)value).isEmpty())
			{
				// We do not save empty collections
				// COLLECTION STATE TRACKING: we could store this (and null) as an out-of-band property
			}
			else
			{
				this.setEntityProperty(entity, value);
			}
		}
	}

	/**
	 * Converts the value into an object suitable for storing in the datastore.  This is
	 * the "parallel" method of LeafSetter; not that because outbound processing of arrays and
	 * collections is so simple, we don't have an object hierarchy to deal with that case.
	 * 
	 * @param value can be any basic type or an array or collection of basic types
	 * @return something that can be saved in the datastore; note that arrays are always
	 *  converted to collections.
	 */
	protected Object prepareForSave(Object value)
	{
		if (value == null)
		{
			return null;
		}
		else if (this.serialize)
		{
			// If it's @Serialized, we serialize it no matter what it looks like
			try
			{
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				ObjectOutputStream oos = new ObjectOutputStream(baos);
				oos.writeObject(value);
				
				return new Blob(baos.toByteArray());
			}
			catch (IOException ex) { throw new RuntimeException(ex); }
		}
		else if (value instanceof String)
		{
			// Check to see if it's too long and needs to be Text instead
			if (((String)value).length() > 500)
			{
				if (this.collectionize)
					throw new IllegalStateException("Objectify cannot autoconvert Strings greater than 500 characters to Text within @Embedded collections." +
							"  You must use Text for the field type instead." +
							"  This is what you tried to save into " + this.field + ": " + value);
				
				return new Text((String)value);
			}
		}
		else if (value instanceof Enum<?>)
		{
			return value.toString();
		}
		else if (value.getClass().isArray())
		{
			if (value.getClass().getComponentType() == Byte.TYPE)
			{
				// Special case!  byte[] gets turned into Blob.
				return new Blob((byte[])value);
			}
			else
			{
				// The datastore cannot persist arrays, but it can persist ArrayList
				int length = Array.getLength(value);
				ArrayList<Object> list = new ArrayList<Object>(length);
				
				for (int i=0; i<length; i++)
					list.add(this.prepareForSave(Array.get(value, i)));
				
				return list;
			}
		}
		else if (value instanceof Collection<?>)
		{
			// All collections get turned into a List that preserves the order.  We must
			// also be sure to convert anything contained in the collection
			ArrayList<Object> list = new ArrayList<Object>(((Collection<?>)value).size());

			for (Object obj: (Collection<?>)value)
				list.add(this.prepareForSave(obj));
			
			return list;
		}
		else if (value instanceof Key<?>)
		{
			return this.factory.typedKeyToRawKey((Key<?>)value);
		}

		// Usually we just want to return the value
		return value;
	}
}
