package com.googlecode.objectify.cmd;

import com.google.cloud.datastore.StructuredQuery;
import com.google.cloud.datastore.Value;
import com.google.common.base.Preconditions;
import com.googlecode.objectify.impl.FilterOperator;
import com.googlecode.objectify.impl.ObjectifyImpl;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.NonFinal;

/**
 * Gives us the ability to compose arbitrarily complex filters with OR and AND sections.
 */
abstract public class Filter {
	/**
	 * Create a filter condition that requires the property to equal the scalar value.
	 */
	public static Filter equalTo(final String property, final Object value) {
		return new EqualToFilter(property, value);
	}

	/**
	 * Create a filter condition that requires the property to not be equal to the scalar value.
	 */
	public static Filter notEqualTo(final String property, final Object value) {
		return new NotEqualToFilter(property, value);
	}

	/**
	 * Create a filter condition that requires the property to be greater than the scalar value
	 */
	public static Filter greaterThan(final String property, final Object value) {
		return new GreaterThanFilter(property, value);
	}

	/**
	 * Create a filter condition that requires the property to be greater than or equal to scalar value
	 */
	public static Filter greaterThanOrEqualTo(final String property, final Object value) {
		return new GreaterThanOrEqualToFilter(property, value);
	}

	/**
	 * Create a filter condition that requires the property to be less than the scalar value
	 */
	public static Filter lessThan(final String property, final Object value) {
		return new LessThanFilter(property, value);
	}

	/**
	 * Create a filter condition that requires the property to be less than or equal to the scalar value
	 */
	public static Filter lessThanOrEqualTo(final String property, final Object value) {
		return new LessThanOrEqualToFilter(property, value);
	}

	/**
	 * Create a filter condition that requires the property to be equal to at least one of a list of scalar
	 * values.
	 *
	 * @param value must be an array or collection
	 */
	public static Filter in(final String property, final Object value) {
		return new InFilter(property, value);
	}

	/**
	 * Create a filter condition that requires the property to NOT be equal to any of a list of scalar
	 * values.
	 *
	 * @param value must be an array or collection
	 */
	public static Filter notIn(final String property, final Object value) {
		return new NotInFilter(property, value);
	}

	// TODO: enable this when the low level API supports OR
//	/**
//	 * Combine an arbitrary list of filter conditions. They can be nested.
//	 */
//	public static Filter or(final Filter... filters) {
//		return new OrFilter(filters);
//	}

	/**
	 * Combine an arbitrary list of filter conditions. They can be nested.
	 */
	public static Filter and(final Filter... filters) {
		return new AndFilter(filters);
	}

	/**
	 * Convert to a low level API filter.
	 */
	abstract public StructuredQuery.Filter convert(final ObjectifyImpl ofyImpl);

	@lombok.Value @NonFinal
	@EqualsAndHashCode(callSuper = false)
	abstract private static class PropertyFilter extends Filter {
		String property;
		Object value;

		@Override
		public final StructuredQuery.Filter convert(final ObjectifyImpl ofyImpl) {
			final Value<?> raw = ofyImpl.makeFilterable(getValue());
			return convert(raw);
		}

		abstract protected StructuredQuery.Filter convert(final Value<?> rawValue);
	}

	@EqualsAndHashCode(callSuper = true)
	@ToString(callSuper = true)
	private static class EqualToFilter extends PropertyFilter {
		public EqualToFilter(final String property, final Object value) {
			super(property, value);
		}

		@Override
		protected StructuredQuery.Filter convert(final Value<?> rawValue) {
			return FilterOperator.EQUAL.of(getProperty(), rawValue);
		}
	}

	@EqualsAndHashCode(callSuper = true)
	@ToString(callSuper = true)
	private static class NotEqualToFilter extends PropertyFilter {
		public NotEqualToFilter(final String property, final Object value) {
			super(property, value);
		}

		@Override
		protected StructuredQuery.Filter convert(final Value<?> rawValue) {
			return FilterOperator.NOT_EQUAL.of(getProperty(), rawValue);
		}
	}

	@EqualsAndHashCode(callSuper = true)
	@ToString(callSuper = true)
	private static class GreaterThanFilter extends PropertyFilter {
		public GreaterThanFilter(final String property, final Object value) {
			super(property, value);
		}

		@Override
		protected StructuredQuery.Filter convert(final Value<?> rawValue) {
			return FilterOperator.GREATER_THAN.of(getProperty(), rawValue);
		}
	}

	@EqualsAndHashCode(callSuper = true)
	@ToString(callSuper = true)
	private static class GreaterThanOrEqualToFilter extends PropertyFilter {
		public GreaterThanOrEqualToFilter(final String property, final Object value) {
			super(property, value);
		}

		@Override
		protected StructuredQuery.Filter convert(final Value<?> rawValue) {
			return FilterOperator.GREATER_THAN_OR_EQUAL.of(getProperty(), rawValue);
		}
	}

	@EqualsAndHashCode(callSuper = true)
	@ToString(callSuper = true)
	private static class LessThanFilter extends PropertyFilter {
		public LessThanFilter(final String property, final Object value) {
			super(property, value);
		}

		@Override
		protected StructuredQuery.Filter convert(final Value<?> rawValue) {
			return FilterOperator.LESS_THAN.of(getProperty(), rawValue);
		}
	}

	@EqualsAndHashCode(callSuper = true)
	@ToString(callSuper = true)
	private static class LessThanOrEqualToFilter extends PropertyFilter {
		public LessThanOrEqualToFilter(final String property, final Object value) {
			super(property, value);
		}

		@Override
		protected StructuredQuery.Filter convert(final Value<?> rawValue) {
			return FilterOperator.LESS_THAN_OR_EQUAL.of(getProperty(), rawValue);
		}
	}

	@EqualsAndHashCode(callSuper = true)
	@ToString(callSuper = true)
	private static class InFilter extends PropertyFilter {
		public InFilter(final String property, final Object value) {
			super(property, value);
		}

		@Override
		protected StructuredQuery.Filter convert(final Value<?> rawValue) {
			return FilterOperator.IN.of(getProperty(), rawValue);
		}
	}

	@EqualsAndHashCode(callSuper = true)
	@ToString(callSuper = true)
	private static class NotInFilter extends PropertyFilter {
		public NotInFilter(final String property, final Object value) {
			super(property, value);
		}

		@Override
		protected StructuredQuery.Filter convert(final Value<?> rawValue) {
			return FilterOperator.NOT_IN.of(getProperty(), rawValue);
		}
	}

	@lombok.Value @NonFinal
	@EqualsAndHashCode(callSuper = false)
	abstract private static class CompositeFilter extends Filter {
		Filter[] filters;

		public CompositeFilter(final Filter[] filters) {
			Preconditions.checkArgument(filters.length >= 1, "You must include at least one condition");
			this.filters = filters;
		}
	}

	@EqualsAndHashCode(callSuper = true)
	@ToString(callSuper = true)
	private static class AndFilter extends CompositeFilter {
		public AndFilter(final Filter[] filters) {
			super(filters);
		}

		@Override
		public StructuredQuery.Filter convert(final ObjectifyImpl ofyImpl) {
			final StructuredQuery.Filter first = getFilters()[0].convert(ofyImpl);

			final StructuredQuery.Filter[] rest = new StructuredQuery.Filter[getFilters().length - 1];
			for (int i = 1; i < getFilters().length; i++) {
				rest[i - 1] = getFilters()[i].convert(ofyImpl);
			}

			return StructuredQuery.CompositeFilter.and(first, rest);
		}
	}

	@EqualsAndHashCode(callSuper = true)
	@ToString(callSuper = true)
	private static class OrFilter extends CompositeFilter {
		public OrFilter(final Filter[] filters) {
			super(filters);
		}

		@Override
		public StructuredQuery.Filter convert(final ObjectifyImpl ofyImpl) {
			final StructuredQuery.Filter first = getFilters()[0].convert(ofyImpl);

			final StructuredQuery.Filter[] rest = new StructuredQuery.Filter[getFilters().length - 1];
			for (int i = 1; i < getFilters().length; i++) {
				rest[i - 1] = getFilters()[i].convert(ofyImpl);
			}

			//return StructuredQuery.CompositeFilter.or(first, rest);
			throw new UnsupportedOperationException("OR is not yet available in the low-level API. Coming soon.");
		}
	}
}
