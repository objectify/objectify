package com.googlecode.objectify.util.cmd;

import com.googlecode.objectify.cmd.Query;

/**
 * Simple wrapper/decorator for a Query.  Use it like this:
 * {@code class MyQuery<T> extends QueryWrapper<MyQuery<T>, T>} 
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class QueryWrapper<H extends QueryWrapper<H, T>, T> extends SimpleQueryWrapper<H, T> implements Query<T>
{
	/** */
	Query<T> base;
	
	/** */
	public QueryWrapper(Query<T> base) 
	{
		super(base);
		this.base = base;
	}
	
	@Override
	public H filter(String condition, Object value)
	{
		H next = this.clone();
		next.base = base.filter(condition, value);
		return next;
	}
	
	@Override
	public H order(String condition)
	{
		H next = this.clone();
		next.base = base.order(condition);
		return next;
	}
}
