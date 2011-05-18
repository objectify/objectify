package com.googlecode.objectify.impl.save;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;

import com.google.appengine.api.datastore.Blob;
import com.google.appengine.api.datastore.Entity;
import com.googlecode.objectify.annotation.Serialized;
import com.googlecode.objectify.impl.TypeUtils;
import com.googlecode.objectify.impl.conv.Conversions;
import com.googlecode.objectify.impl.conv.ConverterSaveContext;

/**
 * <p>Saver which knows how to save basic leaf values. Leaf values are things that
 * go into the datastore: basic types or collections of basic types.  Basically
 * anything except an @Embedded.</p>
 */
public class LeafFieldSaver extends FieldSaver implements ConverterSaveContext
{
	/** */
	Conversions conversions;
	
	/** If true, we add values to a collection inside the entity */
	boolean collectionize;
	
	/** If true, we serialize the value into a Blob */
	boolean serialize;
	
	/** If true, null values are not saved. Leaf collection types are treated this way. */
	boolean ignoreIfNull;
	
	/**
	 * @param field must be a noncollection, nonarray type if collectionize is true
	 * @param ignoreClassIndexing see the FieldSaver javadocs
	 * @param collectionize when true will cause this leaf saver to persist simple basic
	 *  types in a collection inside the entity property.  If set is called multiple times,
	 *  the collection will be appended to. 
	 */
	public LeafFieldSaver(Conversions conv, Class<?> examinedClass, Field field, boolean ignoreClassIndexing, boolean collectionize)
	{
		super(examinedClass, field, ignoreClassIndexing, collectionize);
		
		this.conversions = conv;
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
	 * @see com.googlecode.objectify.impl.save.FieldSaver#saveValue(java.lang.Object, com.google.appengine.api.datastore.Entity, boolean)
	 */
	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void saveValue(Object value, Entity entity, Path path, boolean index)
	{
		value = this.prepareForSave(value);
		
		// Maybe we are supposed to ignore this
		if (value == null && this.ignoreIfNull)
			return;
		
		if (this.collectionize)
		{
			Collection<Object> savedCollection = (Collection<Object>)entity.getProperty(path.toPathString());
			if (savedCollection == null)
			{
				savedCollection = new ArrayList<Object>();
				this.setEntityProperty(entity, savedCollection, path, index);
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
				this.setEntityProperty(entity, value, path, index);
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
		// For now, special case serialization
		if (this.serialize && value != null)
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
		else
		{
			// Run it through the conversions.
			return this.conversions.forDatastore(value, this);
		}
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.conv.ConverterContext#isCollectionizing()
	 */
	@Override
	public boolean inEmbeddedCollection()
	{
		return this.collectionize;
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.conv.ConverterSaveContext#getField()
	 */
	@Override
	public Field getField()
	{
		return this.field;
	}
}
