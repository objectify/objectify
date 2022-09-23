package com.googlecode.objectify.impl;

import com.google.cloud.datastore.ListValue;
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
	EQUAL(PropertyFilter::eq),
	NOT_EQUAL(PropertyFilter::neq),
	IN(requireList(PropertyFilter::in)),
	;

	private final BiFunction<String, Value<?>, PropertyFilter> creator;

	/**
	 */
	public PropertyFilter of(final String propertyName, final Value<?> value) {
		return creator.apply(propertyName, value);
	}

	/**
	 * Generate a creator that makes sure that the value is a list of some sort.
	 */
	private static BiFunction<String, Value<?>, PropertyFilter> requireList(final BiFunction<String, ListValue, PropertyFilter> target) {
		return (property, value) -> {
			if (value instanceof ListValue) {
				return target.apply(property, (ListValue)value);
			} else {
				throw new IllegalArgumentException("Filter operation on '" + property + "' expected a list-type property");
				// Alternatively, convert it?
				//return target.apply(property, ListValue.of(value));
			}
		};
	}
}