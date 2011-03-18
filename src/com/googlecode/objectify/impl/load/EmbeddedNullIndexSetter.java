package com.googlecode.objectify.impl.load;

import java.util.Collection;

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
	public EmbeddedNullIndexSetter(EmbeddedMultivalueSetter impl, String basePath, Collection<String> collisionPaths)
	{
		super(collisionPaths);
		
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
		
		// There is a messy short-circuit we need to perform on an edge case - when we are setting
		// an embedded collection that contains only nulls.  This setter is not called organically,
		// it is called because the EmbeddedNullIndexSetter added a doneHandler to patch up the
		// field with the relevant nulls.  Unfortunately the normal pending-based list system
		// also relies on adding a doneHandler, which screws up 
		
		context.addDoneHandler(new Runnable() {
			@Override
			public void run()
			{
				if (!context.hasPendingEmbeddedMultivalue(basePath))
				{
					// Note that we can't just call implementation.set(toPojo, Collections.EMPTY_LIST, context)
					// because the EmbeddedMultivalueSetter relies on adding a doneHandler, and we're already
					// executing a doneHandler... so we would trip a ConcurrentModificationException on the iterator.
					// No big deal, it's more efficient to do things this way.
					
					// In this case, we know that the indexes must start at 0 and be contiguous.
					int nullCount = ((Collection<?>)value).size();
					
					Collection<Object> embeddedMultivalue = implementation.getOrCreateCollection(toPojo, nullCount);
					
					for (int i=0; i<nullCount; i++)
						embeddedMultivalue.add(null);
				}
			}
		});
	}
}
