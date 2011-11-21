package com.googlecode.objectify.impl.load;

import java.lang.reflect.Field;

/**
 * <p>Most loaders are related to a particular type of field.  This provides a convenient base class.</p>
 */
abstract public class FieldLoader implements Loader {
	
	/** */
	Field field;

	/**
	 */
	public FieldLoader(Field field) {
		this.field = field;
	}
	
	/** */
	public String getFieldName() {
		return field.getName();
	}
}
