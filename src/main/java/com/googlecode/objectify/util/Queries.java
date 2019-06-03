package com.googlecode.objectify.util;

import com.google.cloud.datastore.ProjectionEntityQuery;
import com.google.cloud.datastore.StructuredQuery;
import com.google.cloud.datastore.StructuredQuery.OrderBy;
import com.googlecode.objectify.NamespaceManager;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.function.Supplier;

/**
 * Some static utility methods for interacting with basic datastore objects like keys and queries.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Queries
{
	/**
	 * Copy all the behavior characteristics of the orignial query into the builder.
	 */
	public static <T, B extends StructuredQuery.Builder<T>> B clone(final StructuredQuery<?> orig, final Supplier<B> into) {
		final B builder = into.get();

		builder.setNamespace(orig.getNamespace());
		builder.setEndCursor(orig.getEndCursor());
		builder.setFilter(orig.getFilter());
		builder.setKind(orig.getKind());
		builder.setLimit(orig.getLimit());
		builder.setOffset(orig.getOffset());
		builder.setStartCursor(orig.getStartCursor());

		addOrderBy(builder, orig.getOrderBy());

		return builder;
	}

	/**
	 * The Builder api is programmer-hostile
	 */
	public static void addOrderBy(final StructuredQuery.Builder<?> builder, final List<OrderBy> orderBy) {
		if (!orderBy.isEmpty()) {
			builder.addOrderBy(orderBy.get(0), orderBy.subList(1, orderBy.size()).toArray(new OrderBy[orderBy.size() - 1]));
		}
	}

	/**
	 * The Builder api is programmer-hostile
	 */
	public static void addProjection(final ProjectionEntityQuery.Builder builder, final List<String> projection) {
		if (!projection.isEmpty()) {
			builder.addProjection(projection.get(0), projection.subList(1, projection.size()).toArray(new String[projection.size() - 1]));
		}
	}

	/**
	 * The Builder api is programmer-hostile
	 */
	public static void addDistinctOn(final ProjectionEntityQuery.Builder builder, final List<String> distinctOn) {
		if (!distinctOn.isEmpty()) {
			builder.addDistinctOn(distinctOn.get(0), distinctOn.subList(1, distinctOn.size()).toArray(new String[distinctOn.size() - 1]));
		}
	}

	/**
	 * Take into account the thread local namespace setting
	 */
	public static void adjustNamespace(final StructuredQuery.Builder<?> builder, final String namespace) {
		final String ns = namespace != null ? namespace : NamespaceManager.get();
		if (ns != null)
			builder.setNamespace(ns);
	}
}

