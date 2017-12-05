package com.googlecode.objectify.impl;

import com.google.cloud.datastore.StructuredQuery.PropertyFilter;
import com.google.cloud.datastore.Value;
import lombok.RequiredArgsConstructor;

import java.util.function.BiFunction;

/** The cloud sdk filtering API is somewhat hostile to programmatic query generation, so we need this adaptor */
@RequiredArgsConstructor
enum FilterOperator {
	LESS_THAN(PropertyFilter::lt),
	LESS_THAN_OR_EQUAL(PropertyFilter::le),
	GREATER_THAN(PropertyFilter::gt),
	GREATER_THAN_OR_EQUAL(PropertyFilter::ge),
	EQUAL(PropertyFilter::eq);
	//NOT_EQUAL,
	//IN;

	private final BiFunction<String, Value<?>, PropertyFilter> creator;

	/**
	 */
	public PropertyFilter of(final String propertyName, final Value<?> value) {
		return creator.apply(propertyName, value);
	}
}