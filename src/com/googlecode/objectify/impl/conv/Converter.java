package com.googlecode.objectify.impl.conv;


/** 
 * <p>Interface for objects which know how to convert from POJO field types to
 * whatever is necessary for native storage in the datastore.  The datastore
 * Entity has significant limits on what can be saved with setProperty()
 * and inadvertently transmutes some values (eg Integer to Long).  The converter
 * knows how to make it all work.</p>
 * 
 * <p>Converters are assembled in a chain and each one is given a crack at
 * the value.  This is because some converters cover broad ranges of subclasses
 * (eg, Enum) and a simple hashmap lookup is inadequate.  The first converter
 * that returns a non-null value "wins".</p>
 */
public interface Converter
{
	/** 
	 * Convert the value into an object suitable for storage in the datastore.
	 * @param value will never be null
	 * @return null to indicate that this converter does nothing with the value
	 */
	Object toDatastore(Object value, ConverterSaveContext ctx);

	/**
	 * Convert the value into an object suitable for setting on a field with
	 * the specified type.
	 * @param value will never be null
	 * @param fieldType is the type that the value should be converted to
	 * @return null to indicate that this converter does nothing with the value
	 */
	Object toPojo(Object value, Class<?> fieldType, ConverterLoadContext ctx);
}