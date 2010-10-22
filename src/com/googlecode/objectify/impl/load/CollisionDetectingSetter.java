package com.googlecode.objectify.impl.load;

import java.util.Collection;

import com.googlecode.objectify.impl.LoadContext;

/**
 * <p>Base for setters which perform rudimentary collision detection.  This is how
 * {@code @AlsoLoad} values avoid stepping on the normally loaded values.</p> 
 */
abstract public class CollisionDetectingSetter extends Setter
{
	/**
	 * If non-null check this field in the entity to see if we're stepping on someone
	 * else's field.  This prevents {@code AlsoLoad} values from overwriting normal ones.
	 */
	Collection<String> collisionPaths;
	
	/**
	 * @param collisionPaths can be null
	 */
	public CollisionDetectingSetter(Collection<String> collisionPaths)
	{
		this.collisionPaths = collisionPaths;
	}
	
	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.load.Setter#set(java.lang.Object, java.lang.Object, com.googlecode.objectify.impl.LoadContext)
	 */
	public final void set(Object toPojo, Object value, LoadContext context)
	{
		if (this.collisionPaths != null)
			for (String collPath: this.collisionPaths)
				if (context.getEntity().hasProperty(collPath))
					throw new IllegalStateException("Tried to load the same field twice.  Check the path " + collPath + " in entity " + context.getEntity().toString());

		this.safeSet(toPojo, value, context);
	}
	
	/**
	 * Just like set() but called after collision detection is performed.
	 */
	abstract protected void safeSet(Object toPojo, Object value, LoadContext context);
}
