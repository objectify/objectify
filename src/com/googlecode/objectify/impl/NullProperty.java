package com.googlecode.objectify.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

/** 
 * Property to use with translators when there isn't a relevant property; for example, when making a filterable
 * or when creating translator for the root entity.  Don't construct it, just use the INSTANCE.
 */
public class NullProperty extends AbstractProperty
{
	/** Always just use this instance */
	public static final NullProperty INSTANCE = new NullProperty();

	/** */
	private static final Annotation[] EMPTY_ANNOTATIONS = new Annotation[0];
	
	/** Hide constructor */
	private NullProperty() {
		super(NullProperty.class.getSimpleName(), EMPTY_ANNOTATIONS, NullProperty.class.getSimpleName());
	}
	
	@Override
	public String getName() {
		return this.getClass().getSimpleName();
	}
	
	@Override
	public String toString() {
		return this.getClass().getSimpleName();
	}

	@Override
	public Type getType() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void set(Object pojo, Object value) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public Object get(Object pojo) {
		throw new UnsupportedOperationException();
	}
	
	/** Never saved */
	@Override
	public boolean isSaved(Object onPojo) {
		throw new UnsupportedOperationException();
	}

	/** Since we are never saved this is never called */
	@Override
	public Boolean getIndexInstruction(Object onPojo) {
		throw new UnsupportedOperationException();
	}

	/** Never saved, so never has conditions */
	@Override
	public boolean hasIgnoreSaveConditions() {
		throw new UnsupportedOperationException();
	}
}