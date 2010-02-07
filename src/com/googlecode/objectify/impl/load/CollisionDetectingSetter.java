package com.googlecode.objectify.impl.load;

import com.googlecode.objectify.impl.LoadContext;

/**
 * <p>Base for setters which perform rudimentary collision detection.  This is how
 * {@code @OldName} values avoid stepping on the normally loaded values.</p> 
 */
abstract public class CollisionDetectingSetter extends Setter
{
	/**
	 * If non-null check this field in the entity to see if we're stepping on someone
	 * else's field.  This prevents {@code @OldName} values from overwriting normal ones.
	 */
	String collisionPath;
	
	/**
	 * @param collisionPath can be null
	 */
	public CollisionDetectingSetter(String collisionPath)
	{
		this.collisionPath = collisionPath;
	}
	
	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.load.Setter#set(java.lang.Object, java.lang.Object, com.googlecode.objectify.impl.LoadContext)
	 */
	public final void set(Object toPojo, Object value, LoadContext context)
	{
		if (this.collisionPath != null)
			if (context.getEntity().hasProperty(this.collisionPath))
				throw new IllegalStateException("Tried to load the same field twice.  Check the path " + this.collisionPath + " in entity " + context.getEntity().toString());

		this.safeSet(toPojo, value, context);
	}
	
	/**
	 * Just like set() but called after collision detection is performed.
	 */
	abstract protected void safeSet(Object toPojo, Object value, LoadContext context);
}
