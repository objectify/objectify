package com.googlecode.objectify.impl;

import com.google.cloud.datastore.AggregationQuery;
import com.google.cloud.datastore.AggregationResult;
import com.google.cloud.datastore.AggregationResults;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.EntityQuery;
import com.google.cloud.datastore.KeyQuery;
import com.google.cloud.datastore.ProjectionEntityQuery;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.QueryResults;
import com.google.cloud.datastore.StructuredQuery;
import com.google.cloud.datastore.aggregation.Aggregation;
import com.google.cloud.datastore.aggregation.AggregationBuilder;
import com.google.common.collect.Iterables;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.NamespaceManager;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

/**
 * Logic for dealing with queries.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
@Slf4j
@RequiredArgsConstructor
public class QueryEngine {

	private final LoaderImpl loader;
	private final AsyncDatastoreReaderWriter ds;

	/**
	 * Perform a keys-only query.
	 */
	public <T> QueryResults<Key<T>> queryKeysOnly(final KeyQuery query) {
		log.trace("Starting keys-only query");

		return new KeyQueryResults<>(ds.run(query));
	}

	/**
	 * Perform a keys-only plus batch gets.
	 */
	public <T> QueryResults<T> queryHybrid(final KeyQuery query, final int chunkSize) {
		log.trace("Starting hybrid query");

		final QueryResults<Key<T>> results = new KeyQueryResults<>(ds.run(query));

		return new HybridQueryResults<>(loader.createLoadEngine(), results, chunkSize);
	}

	/**
	 * A normal, non-hybrid query
	 */
	public <T> QueryResults<T> queryNormal(final EntityQuery query, final int chunkSize) {
		log.trace("Starting normal query");

		// Normal queries are actually more complex than hybrid queries because we need the fetched entities to
		// be stuffed back into the engine to satisfy @Load instructions without extra fetching. Even though
		// this looks like we're doing hybrid load-by-key operations, the data is pulled from the stuffed values.

		final LoadEngine loadEngine = loader.createLoadEngine();

		final QueryResults<Entity> entityResults = ds.run(query);

		final QueryResults<com.google.cloud.datastore.Key> stuffed = new StuffingQueryResults(loadEngine, entityResults);

		final QueryResults<Key<T>> keyResults = new KeyQueryResults<>(stuffed);

		return new HybridQueryResults<>(loadEngine, keyResults, chunkSize);
	}

	/**
	 * A projection query. Bypasses the session entirely.
	 */
	public <T> QueryResults<T> queryProjection(final ProjectionEntityQuery query) {
		log.trace("Starting projection query");

		final LoadEngine loadEngine = loader.createLoadEngine();

		return new ProjectionQueryResults<>(ds.run(query), loadEngine);
	}

	/**
	 * Run an arbitrary aggregation query.
	 */
	@SneakyThrows
	public AggregationResult queryAggregations(final StructuredQuery<?> query, final Aggregation... aggregations) {
		log.trace("Starting aggregation query");

		final AggregationQuery aggQuery = Query.newAggregationQueryBuilder()
				.setNamespace(NamespaceManager.get())
				.over(query)
				.addAggregations(aggregations)
				.build();

		final AggregationResults results = ds.runAggregation(aggQuery).get();
		return Iterables.getOnlyElement(results);
	}

	/**
	 * Run an arbitrary aggregation query.
	 */
	@SneakyThrows
	public AggregationResult queryAggregations(final StructuredQuery<?> query, final AggregationBuilder<?>... aggregations) {
		log.trace("Starting aggregation query");

		final AggregationQuery aggQuery = Query.newAggregationQueryBuilder()
				.setNamespace(NamespaceManager.get())
				.over(query)
				.addAggregations(aggregations)
				.build();

		final AggregationResults results = ds.runAggregation(aggQuery).get();
		return Iterables.getOnlyElement(results);
	}

// TODO: uncomment when this API lands in the google-cloud-sdk
//	public ExplainResults<Entity> explain(final Query<?> query, final ExplainOptions options) {
//		return ds.run(query, options);
//	}
}