package com.googlecode.objectify.util.cmd;

import com.googlecode.objectify.impl.cmd.LoaderImpl;
import com.googlecode.objectify.impl.cmd.ObjectifyImpl;

/**
 * Simple wrapper/decorator for a Loader.  Use it like this:
 * {@code class MyLoader extends LoaderWrapper<MyLoader>}
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class LoaderWrapper<H extends LoaderWrapper<H>> extends LoaderImpl
{
	/** */
	public LoaderWrapper(ObjectifyImpl ofy) {
		super(ofy);
	}

	@Override
	@SuppressWarnings("unchecked")
	public H group(Class<?>... groups) {
		return (H)super.group(groups);
	}
}
