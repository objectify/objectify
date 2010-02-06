package com.googlecode.objectify.impl.load;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

import com.google.appengine.api.datastore.Entity;
import com.googlecode.objectify.impl.TypeUtils;

/**
 * <p>Setter which knows how to get or instantiate an embedded class, then
 * pass on to the next setter in the chain.</p>
 */
public class EmbeddedClassSetter extends Setter
{
	/** The field which holds the embedded class */
	Field field;
	Constructor<?> ctor;

	/** */
	public EmbeddedClassSetter(Field field)
	{
		this.field = field;
		this.ctor = TypeUtils.getNoArgConstructor(field.getType());
	}
	
	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.load.Setter#set(java.lang.Object, java.lang.Object, com.google.appengine.api.datastore.Entity)
	 */
	@Override
	public void set(Object obj, Object value, Entity fromEntity)
	{
		Object embedded = TypeUtils.field_get(this.field, obj);
		if (embedded == null)
		{
			embedded = TypeUtils.newInstance(ctor);
			TypeUtils.field_set(this.field, obj, embedded);
		}
		
		this.next.set(embedded, value, fromEntity);
	}
}
