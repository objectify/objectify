 package com.googlecode.objectify.cmd;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;


/**
 * <p>After you call Query.keys(), you are executing a keys-only query.  This is the command structure.
 * It offers only terminators; if you wanted additional filtering, add it before keys().</p>
 * 
 * <p>There is one additional method beyond the standard QueryExecute interface:  the {@code asEntities()}
 * method lets you return your keys as partial entities.</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public interface QueryKeys<T> extends QueryExecute<Key<T>>
{
	/**
	 * Gets the first entity in the result set.  Obeys the offset value.
	 * 
	 * @return the first instance in the result, asynchronously.  The Ref will contain null if the result set was empty.
	 *  Because this is a keys-only fetch, the Ref value will hold a partial entity.
	 */
	Ref<T> first();
	
	/**
	 * Instead of getting keys, return the key data as partial entites - POJO objects whose key (id/parent) fields have
	 * been set but are otherwise uninitialized.
	 */
	QueryExecute<T> asEntities();
}
