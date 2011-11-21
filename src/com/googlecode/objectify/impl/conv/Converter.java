package com.googlecode.objectify.impl.conv;


/** 
 * <p>Interface for objects which know how to convert from POJO field types to
 * whatever is necessary for native storage in the datastore.  The datastore
 * Entity has significant limits on what can be saved with setProperty()
 * and inadvertently transmutes some values (eg Integer to Long).  The converter
 * knows how to make it all work.</p>
 */
public interface Converter<P, D>
{
	/** 
	 * Convert the value into an object suitable for storage in the datastore.
	 * 
	 * @param value is what will be found in the POJO
	 * @return whatever native structure should be stored in the appengine datastore
	 */
	D toDatastore(P value, ConverterSaveContext ctx);

	/**
	 * Convert the value into an object suitable for setting on a field with
	 * the specified type.
	 * 
	 * @param value is what was found in the datastore
	 * @return the type which should be set on the pojo field
	 */
	P toPojo(D value, ConverterLoadContext ctx);
}