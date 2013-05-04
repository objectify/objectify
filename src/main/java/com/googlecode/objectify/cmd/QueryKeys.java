 package com.googlecode.objectify.cmd;

import com.googlecode.objectify.Key;


/**
 * <p>After you call Query.keys(), you are executing a keys-only query.  This is the command structure.
 * It offers only terminators; if you wanted additional filtering, add it before keys().</p>
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public interface QueryKeys<T> extends QueryExecute<Key<T>>
{
}
