package com.googlecode.objectify.test.util;

import java.util.Map;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.impl.ObjectifyImpl;

/**
 * Adds some convenience methods.  Most of the tests were written against Objectify 3 and it's a PITA to convert all the calls.
 */
public class TestObjectify extends ObjectifyImpl<TestObjectify>
{
	/** */
	public TestObjectify(ObjectifyFactory fact) {
		super(fact);
	}

	public <E> Key<E> put(E entitity)  {
		return this.save().<E>entity(entitity).now();
	}

	public <E> Map<Key<E>, E> put(E... entities)  {
		return this.save().<E>entities(entities).now();
	}

	public <K> K get(Key<K> key) {
		return this.load().key(key).now();
	}
}
