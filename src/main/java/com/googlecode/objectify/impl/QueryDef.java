package com.googlecode.objectify.impl;

import com.google.cloud.datastore.Cursor;
import com.google.cloud.datastore.EntityQuery;
import com.google.cloud.datastore.KeyQuery;
import com.google.cloud.datastore.ProjectionEntityQuery;
import com.google.cloud.datastore.ProjectionEntityQuery.Builder;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.StructuredQuery;
import com.google.cloud.datastore.StructuredQuery.CompositeFilter;
import com.google.cloud.datastore.StructuredQuery.Filter;
import com.google.cloud.datastore.StructuredQuery.OrderBy;
import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.googlecode.objectify.util.Queries;
import lombok.RequiredArgsConstructor;
import lombok.Value;

import java.util.function.Supplier;

/**
 * The SDK Query hierarchy and associated builders make it hard to convert between keys-only
 * entity, and projection. So we have to store the state of the query ourselves.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
@Value
@RequiredArgsConstructor
public class QueryDef {
	String namespace;
	String kind;
	ImmutableList<String> projection;
	Filter filter;
	ImmutableList<String> distinctOn;
	ImmutableList<OrderBy> orderBy;
	Cursor startCursor;
	Cursor endCursor;
	int offset;
	Integer limit;

	/** To simulate the old distinct(boolean) behavior, this will add all projections to distinct when Query is generated */
	boolean distinctOnAll;

	private <T> ImmutableList<T> concat(final ImmutableList<T> base, final T element) {
		return ImmutableList.<T>builder().addAll(base).add(element).build();
	}

	public QueryDef() {
		this(null, null, ImmutableList.of(), null, ImmutableList.of(), ImmutableList.of(), null, null, 0, null, false);
	}

	public QueryDef namespace(final String namespace) {
		return new QueryDef(namespace, kind, projection, filter, distinctOn, orderBy, startCursor, endCursor, offset, limit, distinctOnAll);
	}

	public QueryDef kind(final String kind) {
		return new QueryDef(namespace, kind, projection, filter, distinctOn, orderBy, startCursor, endCursor, offset, limit, distinctOnAll);
	}

	public QueryDef project(final String projection) {
		return new QueryDef(namespace, kind, concat(this.projection, projection), filter, distinctOn, orderBy, startCursor, endCursor, offset, limit, distinctOnAll);
	}

	public QueryDef filter(final Filter filter) {
		return new QueryDef(namespace, kind, projection, filter, distinctOn, orderBy, startCursor, endCursor, offset, limit, distinctOnAll);
	}

	/** Convenince method that creates a composite filter with any existing filter (if present) */
	public QueryDef andFilter(final Filter addFilter) {
		return filter(this.filter == null ? addFilter : CompositeFilter.and(this.filter, addFilter));
	}

	public QueryDef distinctOn(final String distinctOn) {
		return new QueryDef(namespace, kind, projection, filter, concat(this.distinctOn, distinctOn), orderBy, startCursor, endCursor, offset, limit, distinctOnAll);
	}

	/**
	 * At the last minute add distinct on all projected fields. This can be called before the projections are added.
	 */
	public QueryDef distinctOnAll(final boolean distinctOnAll) {
		return new QueryDef(namespace, kind, projection, filter, distinctOn, orderBy, startCursor, endCursor, offset, limit, distinctOnAll);
	}

	public QueryDef orderBy(final OrderBy orderBy) {
		return new QueryDef(namespace, kind, projection, filter, distinctOn, concat(this.orderBy, orderBy), startCursor, endCursor, offset, limit, distinctOnAll);
	}

	public QueryDef startCursor(final Cursor startCursor) {
		return new QueryDef(namespace, kind, projection, filter, distinctOn, orderBy, startCursor, endCursor, offset, limit, distinctOnAll);
	}

	public QueryDef endCursor(final Cursor endCursor) {
		return new QueryDef(namespace, kind, projection, filter, distinctOn, orderBy, startCursor, endCursor, offset, limit, distinctOnAll);
	}

	public QueryDef offset(final int offset) {
		return new QueryDef(namespace, kind, projection, filter, distinctOn, orderBy, startCursor, endCursor, offset, limit, distinctOnAll);
	}

	public QueryDef limit(final Integer limit) {
		return new QueryDef(namespace, kind, projection, filter, distinctOn, orderBy, startCursor, endCursor, offset, limit, distinctOnAll);
	}

	public KeyQuery newKeyQuery() {
		Preconditions.checkState(projection.isEmpty(), "You cannot create a keys-only query with a projection");
		return build(Query::newKeyQueryBuilder).build();
	}

	public EntityQuery newEntityQuery() {
		Preconditions.checkState(projection.isEmpty(), "You cannot create an entity query with a projection");
		return build(Query::newEntityQueryBuilder).build();
	}

	public ProjectionEntityQuery newProjectionQuery() {
		Preconditions.checkState(!projection.isEmpty(), "You must have projections to create a projection query");

		final Builder builder = build(Query::newProjectionEntityQueryBuilder);
		Queries.addProjection(builder, projection);
		Queries.addDistinctOn(builder, distinctOnAll ? projection : distinctOn);

		return builder.build();
	}

	private <B extends StructuredQuery.Builder<?>> B build(final Supplier<B> into) {
		final B builder = into.get();

		Queries.adjustNamespace(builder, namespace);

		builder
				.setKind(kind)
				.setFilter(filter)
				.setStartCursor(startCursor)
				.setEndCursor(endCursor)
				.setOffset(offset)
				.setLimit(limit);

		Queries.addOrderBy(builder, orderBy);

		return builder;
	}

	/** Low-cardinality summary */
	public String toStringSummary() {
		final ToStringHelper helper = MoreObjects.toStringHelper("");

		if (namespace != null) {
			helper.add("namespace", namespace);
		}

		if (kind != null) {
			helper.add("kind", kind);
		}

		if (!projection.isEmpty()) {
			helper.add("projection", projection);
		}

		if (!distinctOn.isEmpty()) {
			helper.add("distinctOn", distinctOn);
		}

		if (!orderBy.isEmpty()) {
			helper.add("orderBy", orderBy);
		}

		if (distinctOnAll) {
			helper.add("distinctOnAll", true);
		}

		return helper.toString();
	}

	/** Excludes anything that might be PII */
	public String toStringSanitized() {
		final ToStringHelper helper = MoreObjects.toStringHelper("");

		if (namespace != null) {
			helper.add("namespace", namespace);
		}

		if (kind != null) {
			helper.add("kind", kind);
		}

		if (!projection.isEmpty()) {
			helper.add("projection", projection);
		}

		if (filter != null) {
			helper.add("filter", "[redacted]");
		}

		if (!distinctOn.isEmpty()) {
			helper.add("distinctOn", distinctOn);
		}

		if (!orderBy.isEmpty()) {
			helper.add("orderBy", orderBy);
		}

		if (startCursor != null) {
			helper.add("startCursor", startCursor);
		}

		if (endCursor != null) {
			helper.add("endCursor", endCursor);
		}

		if (offset > 0) {
			helper.add("offset", offset);
		}

		if (limit != null) {
			helper.add("limit", limit);
		}

		if (distinctOnAll) {
			helper.add("distinctOnAll", true);
		}

		return helper.toString();
	}
}
