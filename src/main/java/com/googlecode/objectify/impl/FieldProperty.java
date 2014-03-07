package com.googlecode.objectify.impl;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.annotation.IgnoreSave;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.Unindex;
import com.googlecode.objectify.condition.Always;
import com.googlecode.objectify.condition.If;

/** 
 * Property which encapsulates a simple field. 
 */
public class FieldProperty extends AbstractProperty
{
	Field field;
	
	/** These are authoritative */
	If<?, ?>[] indexConditions;
	If<?, ?>[] unindexConditions;
	If<?, ?>[] ignoreSaveConditions;
	
	/** If we have an @IgnoreSave and it isn't Always */
	boolean hasIgnoreSaveConditions;
	
	private static final MethodHandles.Lookup lookup = MethodHandles.lookup();
	private static final Map<Field, MethodHandle> setHandles = new HashMap<>();
	private static final Map<Field, MethodHandle> getHandles = new HashMap<>();
	
	/**
	 * @param examinedClass is the actual top level concrete class we are examining; the field might
	 * be declared on a superclass of this class so it's not the same as field.getDeclaringClass()
	 */
	public FieldProperty(ObjectifyFactory fact, Class<?> examinedClass, Field field) {
		super(field.getName(), field.getAnnotations(), field);
		
		this.field = field;

		field.setAccessible(true);
		
		IfConditionGenerator ifGenerator = new IfConditionGenerator(fact, examinedClass);

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
		if (ignoreSave != null) {
			hasIgnoreSaveConditions = ignoreSave.value().length != 1 || ignoreSave.value()[0] != Always.class;
			ignoreSaveConditions = ifGenerator.generateIfConditions(ignoreSave.value(), field);
		}
	}
	
	/** */
	@Override
	public Type getType() { return this.field.getGenericType(); }

	/** */
	@Override
	public void set(Object pojo, Object value) {
		try {
			MethodHandle mh = setHandles.get(field);
			if (mh == null) {
				mh = lookup.unreflectSetter(field);
				setHandles.put(field, mh);
			}
			mh.invoke(pojo, value);
		}
		catch (Throwable ex) { 
			throw new RuntimeException(ex); 
		}
	}
	
	/** */
	@Override
	public Object get(Object pojo) {
		try { 
			MethodHandle mh = getHandles.get(field);
			if (mh == null) {
				mh = lookup.unreflectGetter(field);
				getHandles.put(field, mh);
			}
			return mh.invoke(field.getDeclaringClass().cast(pojo)); 
		}
		catch (Throwable ex) { 
			throw new RuntimeException(ex); 
		}
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
		else {
			// check the declared class for instruction
			Index ind = field.getDeclaringClass().getAnnotation(Index.class);
			Unindex unind = field.getDeclaringClass().getAnnotation(Unindex.class);
			if (ind != null && unind != null)
				throw new IllegalStateException("You cannot have @Index and @Unindex on the same class: " + field.getDeclaringClass());
			
			return (ind != null) ? Boolean.TRUE : ((unind != null) ? Boolean.FALSE : null);
		}
	}
	
	/** */
	@Override
	public boolean hasIgnoreSaveConditions() {
		return hasIgnoreSaveConditions;
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
		
		for (int i=0; i<conditions.length; i++) {
			@SuppressWarnings("unchecked")
			If<Object, Object> cond = (If<Object, Object>)conditions[i];
			
			if (cond.matchesValue(value))
				return true;
			
			if (cond.matchesPojo(onPojo))
				return true;
		}

		return false;
	}
}