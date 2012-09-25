package com.googlecode.objectify.condition;



/**
 * <p>Satisfies the opposite of IfEmpty - returns true if the value
 * is not null and not an empty String, Collection, or Map.</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class IfNotEmpty extends ValueIf<Object>
{
	IfEmpty opposite = new IfEmpty();
	
	@Override
	public boolean matchesValue(Object value) {
		return !opposite.matchesValue(value);
	}
}