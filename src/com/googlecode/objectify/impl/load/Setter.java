package com.googlecode.objectify.impl.load;

import com.googlecode.objectify.impl.LoadContext;

/**
 * <p>A setter knows how to set a value in an object graph.  It may be composed of
 * a variety of internal setters that know how to set a value deep in an object graph.
 * The value being set is a <strong>leaf</strong> value from the datastore; that is,
 * something that it persists directly (basic type or collection of basic types).</p>
 * 
 * <p>For example, imagine a Setter for a Person entity with property "name.firstName".</p>
 * <ul>
 * <li>Transmog will find a property in the Entity called "name.firstName" with value "Bob".</li>
 * <li>Transmog will look up a Setter for "name.firstName".</li>
 * <li>Transmog will call setter.set(rootPojo, "Bob")</li>
 * <li>The EmbeddedClassSetter will create a Person object in rootPojo.name</li>
 * <li>The EmbeddedClassSetter will delegate to a LeafSetter, passing in the Person</li>
 * <li>The LeafSetter will set the name field on the Person object.
 * </ul>
 * 
 * <p>Setters are a linear chain like a linked list.  They are also immutable.  You
 * extend the chain by calling extend(), passing in the new tail; this produces an
 * entirely new list.</p> 
 */
abstract public class Setter implements Cloneable
{
	/** The next setter to execute in the chain */
	Setter next;
	
	/**
	 * Called by the Transmog to set a value on an object.  Might actually delegate to
	 * some composite setter to actually set a value deep in the structure.
	 */
	abstract public void set(Object toPojo, Object value, LoadContext context);
	
	/**
	 * @return the next setter in the chain, or null if there is none
	 */
	public Setter getNext() { return this.next; }
	
	/**
	 * Extends the whole chain, adding a setter to the tail.  Since the setters
	 * are immutable, this returns an entirely new list with the tail at the end. 
	 */
	public Setter extend(Setter tail)
	{
		Setter cloned = this.clone();
		
		Setter traverse = cloned;
		while (traverse.next != null)
			traverse = traverse.next;
		
		traverse.next = tail;
		
		return cloned;
	}
	
	/**
	 * Create a copy of this Setter and all child setters in the chain.
	 */
	@Override
	public Setter clone()
	{
		try
		{
			Setter cloned = (Setter)super.clone();
			if (cloned.next != null)
				cloned.next = cloned.next.clone();
			
			return cloned;
		}
		catch (CloneNotSupportedException e) { throw new RuntimeException(e); }
	}
}
