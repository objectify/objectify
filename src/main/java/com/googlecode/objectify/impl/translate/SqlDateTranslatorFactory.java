package com.googlecode.objectify.impl.translate;

import com.googlecode.objectify.impl.Path;

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
	protected ValueTranslator<java.sql.Date, java.util.Date> createValueTranslator(TypeKey<java.sql.Date> tk, CreateContext ctx, Path path) {
		return new ValueTranslator<java.sql.Date, java.util.Date>(java.util.Date.class) {
			@Override
			protected java.sql.Date loadValue(java.util.Date value, LoadContext ctx, Path path) throws SkipException {
				return new java.sql.Date(value.getTime());
			}

			@Override
			protected java.util.Date saveValue(java.sql.Date value, boolean index, SaveContext ctx, Path path) throws SkipException {
				return new java.util.Date(value.getTime());
			}
		};
	}
}
