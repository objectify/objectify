package com.googlecode.objectify.impl.translate;

import java.lang.reflect.Type;

import com.googlecode.objectify.impl.Path;
import com.googlecode.objectify.impl.Property;

/**
 * The datastore can't store java.sql.Date, but it can do java.util.Date.
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org> 
 */
public class SqlDateTranslatorFactory extends ValueTranslatorFactory<java.sql.Date, java.util.Date>
{
	/** */
	public SqlDateTranslatorFactory() {
		super(java.sql.Date.class);
	}
	
	@Override
	protected ValueTranslator<java.sql.Date, java.util.Date> createSafe(Path path, Property property, Type type, CreateContext ctx)
	{
		return new ValueTranslator<java.sql.Date, java.util.Date>(path, java.util.Date.class) {
			@Override
			protected java.util.Date saveValue(java.sql.Date value, SaveContext ctx) {
				return new java.util.Date(value.getTime());
			}
			
			@Override
			protected java.sql.Date loadValue(java.util.Date value, LoadContext ctx) {
				return new java.sql.Date(value.getTime());
			}
		};
	}
}