package com.googlecode.objectify.impl;

import com.google.cloud.datastore.Cursor;
import com.google.cloud.datastore.KeyQuery;
import com.google.cloud.datastore.QueryResults;
import com.google.cloud.datastore.StringValue;
import com.google.cloud.datastore.StructuredQuery.Filter;
import com.google.cloud.datastore.StructuredQuery.OrderBy;
import com.google.cloud.datastore.StructuredQuery.PropertyFilter;
import com.google.cloud.datastore.Value;
import com.google.common.base.MoreObjects;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.LoadResult;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.annotation.Subclass;
import com.googlecode.objectify.cmd.Query;
import com.googlecode.objectify.cmd.QueryResultIterable;
import com.googlecode.objectify.impl.translate.ClassTranslator;
import com.googlecode.objectify.util.IteratorFirstResult;
import com.googlecode.objectify.util.MakeListResult;
import com.googlecode.objectify.util.ResultProxy;
import lombok.SneakyThrows;

import java.util.Iterator;
import java.util.List;

/**
 * Implementation of Query.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class QueryImpl<T> extends SimpleQueryImpl<T> implements Query<T>, Cloneable
{
	/**
	 * Because we process @Load batches, we need to always work in chunks.  So we should always specify
	 * a chunk size to the query.  This is the default if user does not specify an explicit chunk size.
	 */
	static final int DEFAULT_CHUNK_SIZE = 30;

	/** Track this so we can prevent attempts to filter/sort by id or parent */
	private Class<T> classRestriction;

	/**  */
	private QueryDef actual;

	/** */
	private Integer chunk;

	/** Three states; null is "figure it out automatically" */
	private Boolean hybrid;

	/** */
	QueryImpl(final LoaderImpl loader) {
		super(loader);
		this.actual = new QueryDef();
	}

	/** */
	QueryImpl(final LoaderImpl loader, final String kind, final Class<T> clazz) {
		super(loader);

		this.actual = new QueryDef()
				.kind(kind)
				.namespace(loader.getObjectifyImpl().getOptions().getNamespace());

		// If this is a polymorphic subclass, add an extra filter
		if (clazz != null) {
			final Subclass sub = clazz.getAnnotation(Subclass.class);
			if (sub != null) {
				final String discriminator = sub.name().length() > 0 ? sub.name() : clazz.getSimpleName();
				this.addFilter(FilterOperator.EQUAL.of(ClassTranslator.DISCRIMINATOR_INDEX_PROPERTY, StringValue.of(discriminator)));
			}

			this.classRestriction = clazz;
		}
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.cmd.QueryBase#createQuery()
	 */
	@Override
	QueryImpl<T> createQuery() {
		return this.clone();
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Query#filter(java.lang.String, java.lang.Object)
	 */
	@Override
	public QueryImpl<T> filter(final String condition, final Object value) {
		final QueryImpl<T> q = createQuery();
		q.addFilter(condition, value);
		return q;
	}

	/* */
	@Override
	public QueryImpl<T> filter(final Filter filter) {
		final QueryImpl<T> q = createQuery();
		q.addFilter(filter);
		return q;
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Query#order(java.lang.String)
	 */
	@Override
	public QueryImpl<T> order(final String condition) {
		final QueryImpl<T> q = createQuery();
		q.addOrder(condition);
		return q;
	}

	/** Modifies the instance */
	void addFilter(final String condition, final Object value) {

		final String[] parts = condition.trim().split(" ");
		if (parts.length < 1 || parts.length > 2)
			throw new IllegalArgumentException("'" + condition + "' is not a legal filter condition");

		final String prop = parts[0].trim();
		final FilterOperator op = (parts.length == 2) ? this.translate(parts[1]) : FilterOperator.EQUAL;

		// If we have a class restriction, check to see if the property is the @Parent or @Id. We used to try to convert
		// filtering on the id field to a __key__ query, but that tended to confuse users about the real capabilities
		// of GAE and Objectify. So let's force users to use filterKey() instead.
		if (this.classRestriction != null) {
			final KeyMetadata<?> meta = loader.ofy.factory().keys().getMetadataSafe(this.classRestriction);

			if (prop.equals(meta.getParentFieldName())) {
				throw new IllegalArgumentException("@Parent fields cannot be filtered on. Perhaps you wish to use filterKey() or ancestor() instead?");
			}
			else if (prop.equals(meta.getIdFieldName())) {
				throw new IllegalArgumentException("@Id fields cannot be filtered on. Perhaps you wish to use filterKey() instead?");
			}
		}

		// Convert to something filterable, possibly extracting/converting keys
		final Value<?> translated = loader.getObjectifyImpl().makeFilterable(value);

		addFilter(op.of(prop, translated));
	}

	/**
	 * Add the filter as an AND to whatever is currently set as the actual filter.
	 */
	void addFilter(final Filter filter) {
		actual = actual.andFilter(filter);
	}

	/**
	 * Converts the textual operator (">", "<=", etc) into a FilterOperator.
	 * Forgiving about the syntax; != and <> are NOT_EQUAL, = and == are EQUAL.
	 */
	protected FilterOperator translate(String operator) {
		operator = operator.trim();

		if (operator.equals("=") || operator.equals("=="))
			return FilterOperator.EQUAL;
		else if (operator.equals(">"))
			return FilterOperator.GREATER_THAN;
		else if (operator.equals(">="))
			return FilterOperator.GREATER_THAN_OR_EQUAL;
		else if (operator.equals("<"))
			return FilterOperator.LESS_THAN;
		else if (operator.equals("<="))
			return FilterOperator.LESS_THAN_OR_EQUAL;
		else if (operator.equals("!=") || operator.equals("<>"))
			//return FilterOperator.NOT_EQUAL;
			throw new UnsupportedOperationException("The Cloud Datastore SDK does not currently support 'NOT EQUAL' filters");
		else if (operator.toLowerCase().equals("in"))
			//return FilterOperator.IN;
			throw new UnsupportedOperationException("The Cloud Datastore SDK does not currently support 'IN' filters");
		else
			throw new IllegalArgumentException("Unknown operator '" + operator + "'");
	}

	/** Modifies the instance */
	void addOrder(String condition) {
		condition = condition.trim();
		boolean descending = false;

		if (condition.startsWith("-")) {
			descending = true;
			condition = condition.substring(1).trim();
		}

		// Prevent ordering by @Id or @Parent fields, which are really part of the key
		if (this.classRestriction != null) {
			final KeyMetadata<?> meta = loader.ofy.factory().keys().getMetadataSafe(this.classRestriction);

			if (condition.equals(meta.getParentFieldName()))
				throw new IllegalArgumentException("You cannot order by @Parent field. Perhaps you wish to order by __key__ instead?");

			if (condition.equals(meta.getIdFieldName())) {
				throw new IllegalArgumentException("You cannot order by @Id field. Perhaps you wish to order by __key__ instead?");
			}
		}

		this.actual = actual.orderBy(descending ? OrderBy.desc(condition) : OrderBy.asc(condition));
	}

	/** Modifies the instance */
	void setAncestor(final Object keyOrEntity) {
		final com.google.cloud.datastore.Key key = loader.ofy.factory().keys().anythingToRawKey(keyOrEntity, loader.ofy.getOptions().getNamespace());
		this.actual = this.actual.andFilter(PropertyFilter.hasAncestor(key));
	}

	/** Modifies the instance */
	void setLimit(final int value) {
		this.actual = this.actual.limit(value);

		if (this.chunk == null)
			this.chunk = value;
	}

	/** Modifies the instance */
	void setOffset(final int value) {
		this.actual = this.actual.offset(value);
	}

	/** Modifies the instance */
	void setStartCursor(final Cursor value) {
		this.actual = this.actual.startCursor(value);
	}

	/** Modifies the instance */
	void setEndCursor(final Cursor value) {
		this.actual = this.actual.endCursor(value);
	}

	/** Modifies the instance */
	void setChunk(final int value) {
		this.chunk = value;
	}

	/** Modifies the instance */
	void setHybrid(final boolean force) {
		this.hybrid = force;
	}

	/** Just a sanity check */
	void checkKeysOnlyOk() {
		if (!this.actual.getProjection().isEmpty())
			throw new IllegalStateException("You cannot ask for both keys-only and projections in the same query. That makes no sense!");
	}

	/** Modifies the instance */
	void setDistinct(final boolean value) {
		this.actual = this.actual.distinctOnAll(value);
	}

	/** Modifies the instance */
	void addProjection(final String... fields) {
		if (this.hybrid != null && this.hybrid)
			throw new IllegalStateException("You cannot ask for both hybrid and projections in the same query. That makes no sense!");

		for (final String field: fields) {
			this.actual = this.actual.project(field);
		}
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add("query", actual).toString();
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Query#first()
	 */
	@Override
	public LoadResult<T> first() {
		// By the way, this is the same thing that PreparedQuery.asSingleEntity() does internally
		final Iterator<T> it = this.limit(1).iterator();

		return new LoadResult<>(null, new IteratorFirstResult<>(it));
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Query#count()
	 */
	@Override
	public int count() {
		return loader.createQueryEngine().queryCount(this.actual.newKeyQuery());
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.QueryExecute#iterable()
	 */
	@Override
	public QueryResultIterable<T> iterable() {
		return this::iterator;
	}

	/* (non-Javadoc)
	 * @see com.google.cloud.datastore.QueryResultIterable#iterator()
	 */
	@Override
	public QueryResults<T> iterator() {
		if (!actual.getProjection().isEmpty())
			return loader.createQueryEngine().queryProjection(this.actual.newProjectionQuery());
		else if (shouldHybridize())
			return loader.createQueryEngine().queryHybrid(this.actual.newKeyQuery(), chunk == null ? Integer.MAX_VALUE : chunk);
		else
			return loader.createQueryEngine().queryNormal(this.actual.newEntityQuery(), chunk == null ? Integer.MAX_VALUE : chunk);
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Query#list()
	 */
	@Override
	public List<T> list() {
		return ResultProxy.create(List.class, new MakeListResult<>(this.chunk(Integer.MAX_VALUE).iterator()));
	}

	/**
	 * Get an iterator over the keys.  Not part of the public api, but used by QueryKeysImpl.  Assumes
	 * that setKeysOnly() has already been set.
	 */
	QueryResults<Key<T>> keysIterator() {
		final QueryEngine queryEngine = loader.createQueryEngine();
		final KeyQuery query = this.actual.newKeyQuery();
		return queryEngine.queryKeysOnly(query);
	}

	/**
	 * @return true if we should hybridize this query
	 */
	private boolean shouldHybridize() {
		if (hybrid != null)
			return hybrid;

		// If the class is cacheable
		if (classRestriction != null && loader.getObjectifyImpl().getOptions().isCache() && fact().getMetadata(classRestriction).getCacheExpirySeconds() != null)
			return true;

		return false;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#clone()
	 */
	@SuppressWarnings({"unchecked", "CloneDoesntDeclareCloneNotSupportedException"})
	@SneakyThrows
	public QueryImpl<T> clone() {
		return (QueryImpl<T>)super.clone();
	}

	/** Convenience method */
	private ObjectifyFactory fact() {
		return loader.getObjectify().factory();
	}
}
