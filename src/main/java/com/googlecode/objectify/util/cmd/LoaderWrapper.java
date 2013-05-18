package com.googlecode.objectify.util.cmd;

import com.googlecode.objectify.impl.LoaderImpl;
import com.googlecode.objectify.impl.ObjectifyImpl;

/**
 * This is not actually a wrapper; it extends LoaderImpl.  Use it like this:
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
