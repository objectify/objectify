package com.googlecode.objectify.impl;

import java.lang.reflect.Field;
import java.util.Collection;

import javax.persistence.Embedded;

import com.googlecode.objectify.ObjectifyFactory;

/**
 * A loader that knows how to persist a simple field.  The field
 * cannot be an array or collection or an embedded.
 */
public class BasicLoader<T> extends Loader<T>
{
	/** */
	Field field;
	
	/** */
	public BasicLoader(ObjectifyFactory fact, Navigator<T> nav, Field field)
	{
		super(fact, nav);
		
		assert (!field.isAnnotationPresent(Embedded.class));
		assert (!field.getType().isArray());
		assert (!Collection.class.isAssignableFrom(field.getType()));

		this.field = field;
	}
	
	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.Loader#load(java.lang.Object, java.lang.Object)
	 */
	@Override
	public void load(Object intoTarget, Object value)
	{
		TypeUtils.field_set(
				this.field,
				intoTarget,
				TypeUtils.convertFromDatastore(this.factory, value, this.field.getType()));
	}
}
