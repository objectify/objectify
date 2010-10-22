package com.googlecode.objectify.impl.conv;

import com.google.appengine.api.datastore.Text;


/**
 * Knows how to convert Strings 
 */
public class StringConverter implements Converter
{
	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.conv.Converter#toDatastore(java.lang.Object, com.googlecode.objectify.impl.conv.ConverterContext)
	 */
	@Override
	public Object toDatastore(Object value, ConverterSaveContext ctx)
	{
		if (!(value instanceof String))
			return null;
		
		// Check to see if it's too long and needs to be Text instead
		if (((String)value).length() > 500)
		{
			if (ctx.inEmbeddedCollection())
				throw new IllegalStateException("Objectify cannot autoconvert Strings greater than 500 characters to Text within @Embedded collections." +
						"  You must use Text for the field type instead." +
						"  This is what you tried to save into " + ctx.getField() + ": " + value);
			
			return new Text((String)value);
		}
		else
			return value;
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.conv.Converter#toPojo(java.lang.Object, java.lang.Class)
	 */
	@Override
	public Object toPojo(Object value, Class<?> fieldType, ConverterLoadContext ctx)
	{
		if (fieldType != String.class)
			return null;
		
		if (value instanceof Text)
			return ((Text)value).getValue();
		else
			return value.toString();
	}
}