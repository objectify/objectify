package com.googlecode.objectify.impl.load;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Collection;

import com.googlecode.objectify.impl.LoadContext;
import com.googlecode.objectify.impl.TypeUtils;

/**
 * <p>Setter which knows how to get or instantiate an embedded class, then
 * pass on to the next setter in the chain.</p>
 */
public class EmbeddedClassSetter extends CollisionDetectingSetter
{
	/** The field which holds the embedded class */
	Field field;
	Constructor<?> ctor;

	/** */
	public EmbeddedClassSetter(Field field, Collection<String> collisionPaths)
	{
		super(collisionPaths);
		
		this.field = field;
		this.ctor = TypeUtils.getNoArgConstructor(field.getType());
	}
	
	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.load.CollisionDetectingSetter#safeSet(java.lang.Object, java.lang.Object, com.googlecode.objectify.impl.LoadContext)
	 */
	@Override
	protected void safeSet(Object obj, Object value, LoadContext context)
	{
		Object embedded = TypeUtils.field_get(this.field, obj);
		if (embedded == null)
		{
			embedded = TypeUtils.newInstance(ctor);
			TypeUtils.field_set(this.field, obj, embedded);
		}
		
		this.next.set(embedded, value, context);
	}
}
