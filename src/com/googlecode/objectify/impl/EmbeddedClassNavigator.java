package com.googlecode.objectify.impl;

import java.lang.reflect.Field;

import javax.persistence.Embedded;


/**
 * Navigator which knows how to grab a embedded object from a field,
 * creating it if necessary. 
 */
public class EmbeddedClassNavigator<T> extends ChainedNavigator<T>
{
	Field embeddedClassField;
	
	/**
	 * @param embeddedClassField is a field in the previous pojo with an @Embedded annotation.
	 *  It must be a normal class, not an array or collection. 
	 */
	public EmbeddedClassNavigator(Navigator<T> predecessor, Field embeddedClassField)
	{
		super(predecessor);
		
		assert embeddedClassField.isAnnotationPresent(Embedded.class);
		
		this.embeddedClassField = embeddedClassField;
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.Navigator#get(java.lang.Object)
	 */
	@Override
	public Object navigateToTarget(T root)
	{
		Object previousTarget = this.chain.navigateToTarget(root);
		
		Object target = TypeUtils.field_get(this.embeddedClassField, previousTarget);
		if (target == null)
		{
			target = TypeUtils.class_newInstance(this.embeddedClassField.getType());
			TypeUtils.field_set(this.embeddedClassField, previousTarget, target);
		}
		
		return target;
	}
}
