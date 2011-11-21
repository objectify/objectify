package com.googlecode.objectify.impl.conv;



/**
 * The datastore can't store java.sql.Date, but it can do java.util.Date. 
 */
public class SqlDateConverter extends SimpleConverterFactory<java.sql.Date, java.util.Date>
{
	/** */
	public SqlDateConverter() {
		super(java.sql.Date.class);
	}
	
	@Override
	protected Converter<java.sql.Date, java.util.Date> create(Class<?> type, ConverterCreateContext ctx) {
		return new Converter<java.sql.Date, java.util.Date>() {
			
			@Override
			public java.sql.Date toPojo(java.util.Date value, ConverterLoadContext ctx) {
				return new java.sql.Date(value.getTime());
			}
			
			@Override
			public java.util.Date toDatastore(java.sql.Date value, ConverterSaveContext ctx) {
				return new java.util.Date(value.getTime());
			}
		};
	}
}