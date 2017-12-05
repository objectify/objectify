package com.googlecode.objectify.cmd;

import com.google.cloud.datastore.QueryResults;

/**
 * Replaces a class from the old GAE SDK. Not totally compatible; we return QueryResults instead of
 * the (no longer existant) QueryResultIterator.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public interface QueryResultIterable<T> extends Iterable<T>
{
	QueryResults<T> iterator();
}
