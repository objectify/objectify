package com.googlecode.objectify;

import com.google.cloud.datastore.BaseEntity;

/**
 * Thrown when something went wrong during the load translation process; for example, the data in the
 * datastore might be in a format incompatible with the intended pojo field.  Indicates what went
 * wrong with which entity.
 */
public class LoadException extends TranslateException
{
	private static final long serialVersionUID = 1L;
	
	private final BaseEntity<?> entity;

	/** Constructor to use when you're saving an entity with a known key */
	public LoadException(final BaseEntity<?> entity, final String message, final Throwable cause) {
		super("Error loading " + entity.getKey() + ": " + message, cause);
		
		this.entity = entity;
	}
	
	/** The complete version of what we couldn't translate */
	public BaseEntity<?> getEntity() {
		return this.entity;
	}
}
