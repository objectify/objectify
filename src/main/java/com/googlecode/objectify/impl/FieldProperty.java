package com.googlecode.objectify.impl;

import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.annotation.IgnoreSave;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.Unindex;
import com.googlecode.objectify.condition.If;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Type;

/** 
 * Property which encapsulates a simple field. 
 */
public class FieldProperty extends AbstractProperty
{
	Field field;
	MethodHandle getter;
	MethodHandle setter;
	
	/** These are authoritative */
	If<?, ?>[] indexConditions;
	If<?, ?>[] unindexConditions;
	If<?, ?>[] ignoreSaveConditions;
	
	/** If we have an @IgnoreSave and it isn't Always */
	boolean hasIgnoreSaveConditions;
	
	/**
	 * @param examinedClass is the actual top level concrete class we are examining; the field might
	 * be declared on a superclass of this class so it's not the same as field.getDeclaringClass()
	 */
	public FieldProperty(ObjectifyFactory fact, Class<?> examinedClass, Field field) {
		super(field.getName(), field.getAnnotations(), field);

		field.setAccessible(true);
		this.field = field;
		try {
			this.getter = MethodHandles.lookup().unreflectGetter(field);
			this.setter = MethodHandles.lookup().unreflectSetter(field);
		}
		catch (ReflectiveOperationException e) {
			throw new IllegalStateException(e);
		}

		IfConditionGenerator ifGenerator = new IfConditionGenerator(fact);

		// Check @Index and @Unindex conditions
		Index indexedAnn = field.getAnnotation(Index.class);
		Unindex unindexedAnn = field.getAnnotation(Unindex.class);

		if (indexedAnn != null && unindexedAnn != null)
			throw new IllegalStateException("Cannot have @Indexed and @Unindexed on the same field: " + field);
		
		if (indexedAnn != null)
			this.indexConditions = ifGenerator.generateIfConditions(indexedAnn.value(), field);
		
		if (unindexedAnn != null)
			this.unindexConditions = ifGenerator.generateIfConditions(unindexedAnn.value(), field);
		
		// Now watch out for @IgnoreSave conditions
		IgnoreSave ignoreSave = field.getAnnotation(IgnoreSave.class);
		if (ignoreSave != null)
			ignoreSaveConditions = ifGenerator.generateIfConditions(ignoreSave.value(), field);
	}
	
	/** */
	@Override
	public Type getType() { return this.field.getGenericType(); }

	/** */
	@Override
	public void set(Object pojo, Object value) {
		try {
			//this.field.set(pojo, value);
			setter.invoke(pojo, value);
		}
		catch (RuntimeException ex) { throw ex; }
		catch (Throwable ex) { throw new RuntimeException(ex); }
	}
	
	/** */
	@Override
	public Object get(Object pojo) {
		try {
			//return this.field.get(pojo);
			return getter.invoke(pojo);
		}
		catch (RuntimeException ex) { throw ex; }
		catch (Throwable ex) { throw new RuntimeException(ex); }
	}

	/** */
	@Override
	public String toString() {
		return this.field.toString();
	}

	/** */
	@Override
	public boolean isSaved(Object onPojo) {
		return !this.matches(onPojo, ignoreSaveConditions);
	}

	/** */
	@Override
	public Boolean getIndexInstruction(Object onPojo) {
		if (this.matches(onPojo, indexConditions))
			return true;
		else if (this.matches(onPojo, unindexConditions))
			return false;
		else
			return null;
	}
	
	/**
	 * Tests whether a set of conditions match.
	 * @param conditions can be null; this always matches false
	 * @return true if we match the conditions, false if we do not 
	 */
	private boolean matches(Object onPojo, If<?, ?>[] conditions) {
		if (conditions == null)
			return false;
		
		Object value = this.get(onPojo);

		for (If<?, ?> condition: conditions) {
			@SuppressWarnings("unchecked")
			If<Object, Object> cond = (If<Object, Object>)condition;

			if (cond.matchesValue(value))
				return true;

			if (cond.matchesPojo(onPojo))
				return true;
		}

		return false;
	}
}