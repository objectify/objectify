package com.googlecode.objectify.mapper;



/**
 * <p>Used with the @Mapify annotation to convert collections (usually of embedded objects) into maps
 * with arbitrary keys.</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public interface Mapper<K, V>
{
	/** Get whatever should be used as the map key given the specified value */
	K getKey(V value);
}