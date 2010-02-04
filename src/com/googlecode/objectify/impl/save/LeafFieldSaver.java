package com.googlecode.objectify.impl.save;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Text;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.ObjectifyFactory;
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
	
	/**
	 * @param field must be a noncollection, nonarray type if collectionize is true
	 * @param collectionize when true will cause this leaf saver to persist simple basic
	 *  types in a collection inside the entity property.  If set is called multiple times,
	 *  the collection will be appended to. 
	 */
	public LeafFieldSaver(ObjectifyFactory fact, String pathPrefix, Field field, boolean forceUnindexed, boolean collectionize)
	{
		super(pathPrefix, field, forceUnindexed);
		
		if (collectionize)
			if (field.getType().isArray() || Collection.class.isAssignableFrom(field.getType()))
				throw new IllegalStateException("Cannot place array or collection properties inside @Embedded arrays or collections. The offending field is " + field);
		
		this.factory = fact;
		this.collectionize = collectionize;
	}
	
	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.Saver#save(java.lang.Object, com.google.appengine.api.datastore.Entity)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public void save(Object pojo, Entity entity)
	{
		Object value = null;
		
		// If the pojo itself comes in as null, that means we are in an embedded collection
		// and there was a null value in it.  We must insert a placeholder in the array!
		if (pojo == null)
		{
			assert this.collectionize : "Shouldn't be here with a null pojo if we're not in an embedded collection!";
		}
		else
		{
			value = TypeUtils.field_get(this.field, pojo);
			value = this.prepareForSave(value);
		}
		
		if (this.collectionize)
		{
			Collection<Object> savedCollection = (Collection<Object>)entity.getProperty(this.path);
			if (savedCollection == null)
			{
				savedCollection = new ArrayList<Object>();
				this.setEntityProperty(entity, value);
			}
			
			savedCollection.add(value);
		}
		else
			this.setEntityProperty(entity, value);
	}

	/**
	 * Converts the value into an object suitable for storing in the datastore.  This is
	 * the "parallel" method of LeafSetter; not that because outbound processing of arrays and
	 * collections is so simple, we don't have an object hierarchy to deal with that case.
	 * 
	 * @param value can be any basic type or an array or collection of basic types
	 */
	protected Object prepareForSave(Object value)
	{
		if (value == null)
		{
			return null;
		}
		else if (value instanceof String)
		{
			// Check to see if it's too long and needs to be Text instead
			if (((String)value).length() > 500)
				return new Text((String)value);
		}
		else if (value instanceof Enum<?>)
		{
			return value.toString();
		}
		else if (value.getClass().isArray())
		{
			// The datastore cannot persist arrays, but it can persist ArrayList
			int length = Array.getLength(value);
			ArrayList<Object> list = new ArrayList<Object>(length);
			
			for (int i=0; i<length; i++)
				list.add(this.prepareForSave(Array.get(value, i)));
			
			return list;
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
			return this.factory.oKeyToRawKey((Key<?>)value);
		}

		// Usually we just want to return the value
		return value;
	}
}
