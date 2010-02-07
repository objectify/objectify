package com.googlecode.objectify.impl.load;

import java.util.Collections;

import com.googlecode.objectify.impl.LoadContext;

/**
 * <p>This is a special mapping for the ^null property which exists to deal with one
 * edge case:  when an embedded collection is saved that has nothing but nulls.  Since
 * there are no actual values, none of the normal values will be set - just the ^null
 * index property.  This allows us to check for the case.</p>
 */
public class EmbeddedNullIndexSetter extends CollisionDetectingSetter
{
	/** The path of the normal */
	String basePath;
	
	EmbeddedMultivalueSetter implementation;
	
	/** */
	public EmbeddedNullIndexSetter(EmbeddedMultivalueSetter impl, String basePath, String collisionPath)
	{
		super(collisionPath);
		
		this.implementation = impl;
		this.basePath = basePath;
	}
	
	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.load.CollisionDetectingSetter#safeSet(java.lang.Object, java.lang.Object, com.googlecode.objectify.impl.LoadContext)
	 */
	@Override
	public void safeSet(final Object toPojo, final Object value, final LoadContext context)
	{
		// We just need to set a hook for after all the other properties have been
		// loaded.  For a collection containing nothing but nulls, the basePath won't
		// have been processed, and we can force it ourselves.
		
		context.addDoneHandler(new Runnable() {
			@Override
			public void run()
			{
				if (!context.getProcessedEmbeddedMultivaluePaths().contains(basePath))
					implementation.set(toPojo, Collections.EMPTY_LIST, context);
			}
		});
	}
}
