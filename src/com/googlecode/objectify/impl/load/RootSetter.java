package com.googlecode.objectify.impl.load;

import com.googlecode.objectify.impl.LoadContext;

/**
 * <p>An instance of this object makes the construction of setter chains a lot easier.
 * It should be removed from the chain after construction is complete.</p>
 */
public class RootSetter extends Setter
{
	/**
	 * This shouldn't be called and will actually pop an assertion.  However, if assertions
	 * are disabled it will operate just fine.
	 */
	@Override
	public void set(Object obj, Object value, LoadContext context)
	{
		// Might as well let us know that someone screwed up
		assert false: "SetterRoot should have been removed from the setter chain. This is a programmer error.";
	
		// Let things work for anyone with assertions disabled - it will
		this.next.set(obj, value, context);
	}
}
