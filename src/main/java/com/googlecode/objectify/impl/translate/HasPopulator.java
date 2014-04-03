package com.googlecode.objectify.impl.translate;

/**
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public interface HasPopulator<P>
{
	Populator<P> getPopulator();
}
