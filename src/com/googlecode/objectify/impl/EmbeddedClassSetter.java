package com.googlecode.objectify.impl;

import java.lang.reflect.Field;

/**
 * <p>Setter which knows how to get or instantiate an embedded class, then
 * pass on to the next setter in the chain.</p>
 */
public class EmbeddedClassSetter extends Setter
{
	/** The field which holds the embedded class */
	Field field;
	
	/** */
	public EmbeddedClassSetter(Field field)
	{
		this.field = field;
	}
	
	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.Setter#set(java.lang.Object, java.lang.Object)
	 */
	@Override
	public void set(Object obj, Object value)
	{
		Object embedded = TypeUtils.field_get(this.field, obj);
		if (embedded == null)
		{
			embedded = TypeUtils.class_newInstance(this.field.getType());
			TypeUtils.field_set(this.field, obj, embedded);
		}
		
		this.next.set(embedded, value);
	}
}
