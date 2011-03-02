package com.googlecode.objectify.impl.load;


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Collection;

import com.google.appengine.api.datastore.Blob;
import com.googlecode.objectify.impl.LoadContext;
import com.googlecode.objectify.impl.Wrapper;
import com.googlecode.objectify.impl.conv.Conversions;
import com.googlecode.objectify.impl.conv.ConverterLoadContext;

/**
 * <p>Setter which knows how to set any kind of leaf value.  This could be any basic type
 * or a collection of basic types; basically anything except an @Embedded.</p>
 * 
 * <p>This is always the termination of a setter chain; the {@code next} value is ignored.</p>
 */
public class LeafSetter extends CollisionDetectingSetter implements ConverterLoadContext
{
	/** */
	Conversions conversions;
	
	/** The field or method we set */
	Wrapper field;
	
	/** If true, we expect a Blob and need to de-serialize it */
	boolean serialized;
	
	/** */
	public LeafSetter(Conversions conv, Wrapper field, Collection<String> collisionPaths)
	{
		super(collisionPaths);
		
		this.conversions = conv;
		this.field = field;
		this.serialized = field.isSerialized();
	}
	
	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.load.CollisionDetectingSetter#safeSet(java.lang.Object, java.lang.Object, com.googlecode.objectify.impl.LoadContext)
	 */
	@Override
	protected void safeSet(Object obj, Object value, LoadContext context)
	{
		this.field.set(obj, importBasic(value, this.field.getType(), obj));
	}

	/**
	 * Converts a value obtained from the datastore into what gets sent on the field.
	 * The datastore translates values in ways that are not always convenient; for
	 * example, all numbers become Long and booleans become Boolean. This method translates
	 * just the basic types - not collection types.
	 *  
	 * @param fromValue	is the property value that came out of the datastore Entity
	 * @param toType	is the type to convert it to.
	 */
	Object importBasic(Object fromValue, Class<?> toType, Object onPojo)
	{
		// For now, special case serialization
		if (this.serialized && fromValue != null)
		{
			// Above all others, if we're serialized, take the Blob and deserialize it.
			if (!(fromValue instanceof Blob))
				throw new IllegalStateException("Tried to deserialize non-Blob " + fromValue + " for field " + this.field);
			
			try
			{
				ByteArrayInputStream bais = new ByteArrayInputStream(((Blob)fromValue).getBytes());
				ObjectInputStream ois = new ObjectInputStream(bais);
				
				return ois.readObject();
			}
			catch (IOException ex) { throw new RuntimeException(ex); }
			catch (ClassNotFoundException ex) { throw new IllegalStateException("Unable to deserialize " + fromValue + " on field " + this.field + ": " + ex); }
		}
		else
		{
			return this.conversions.forPojo(fromValue, toType, this, onPojo);
		}
	}

	/** Ensure that nobody tries to extend the leaf nodes. */
	@Override
	final public Setter extend(Setter tail)
	{
		throw new UnsupportedOperationException("Objectify bug - cannot extend Leaf setters");
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.conv.ConverterLoadContext#getField()
	 */
	@Override
	public Wrapper getField()
	{
		return this.field;
	}
}
