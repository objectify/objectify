package com.googlecode.objectify.impl;

import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.PropertyProjection;
import com.google.appengine.api.datastore.Query.CompositeFilterOperator;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.SortDirection;
import com.google.appengine.api.datastore.QueryResultIterable;
import com.google.appengine.api.datastore.QueryResultIterator;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.LoadResult;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.annotation.Subclass;
import com.googlecode.objectify.cmd.Query;
import com.googlecode.objectify.impl.translate.ClassTranslator;
import com.googlecode.objectify.util.DatastoreUtils;
import com.googlecode.objectify.util.IteratorFirstResult;
import com.googlecode.objectify.util.MakeListResult;
import com.googlecode.objectify.util.ResultProxy;

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

	/** We need to track this because it enables the ability to filter/sort by id */
	Class<T> classRestriction;

	/** The actual datastore query constructed by this object */
	com.google.appengine.api.datastore.Query actual;

	/** */
	int limit;
	int offset;
	Cursor startAt;
	Cursor endAt;
	Integer chunk;
	boolean hybrid;	// starts false

	/** Need to know this so that we can force hybrid off when we get a multiquery (IN/NOT) or order */
	boolean hasExplicitHybrid;
	boolean hasMulti;

	/** */
	QueryImpl(LoaderImpl<?> loader) {
		super(loader);
		this.actual = new com.google.appengine.api.datastore.Query();
	}

	/** */
	QueryImpl(LoaderImpl<?> loader, Class<T> clazz) {
		super(loader);

		this.actual = new com.google.appengine.api.datastore.Query(Key.getKind(clazz));

		// If this is a polymorphic subclass, add an extra filter
		Subclass sub = clazz.getAnnotation(Subclass.class);
		if (sub != null) {
			String discriminator = sub.name().length() > 0 ? sub.name() : clazz.getSimpleName();
			this.addFilter(FilterOperator.EQUAL.of(ClassTranslator.DISCRIMINATOR_INDEX_PROPERTY, discriminator));
		}

		// If the class is cacheable, hybridize
		if (loader.getObjectifyImpl().getCache() && fact().getMetadata(clazz).getCacheExpirySeconds() != null)
			hybrid = true;

		this.classRestriction = clazz;
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
	public QueryImpl<T> filter(String condition, Object value) {
		QueryImpl<T> q = createQuery();
		q.addFilter(condition, value);
		return q;
	}

	/* */
	@Override
	public QueryImpl<T> filter(Filter filter) {
		QueryImpl<T> q = createQuery();
		q.addFilter(filter);
		return q;
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Query#order(java.lang.String)
	 */
	@Override
	public QueryImpl<T> order(String condition) {
		QueryImpl<T> q = createQuery();
		q.addOrder(condition);
		return q;
	}

	/** @return the underlying datastore query object */
	private com.google.appengine.api.datastore.Query getActualQuery() {
		return this.actual;
	}

	/** Modifies the instance */
	void addFilter(String condition, Object value) {

		String[] parts = condition.trim().split(" ");
		if (parts.length < 1 || parts.length > 2)
			throw new IllegalArgumentException("'" + condition + "' is not a legal filter condition");

		String prop = parts[0].trim();
		FilterOperator op = (parts.length == 2) ? this.translate(parts[1]) : FilterOperator.EQUAL;

		// If we have a class restriction, check to see if the property is the @Parent or @Id. We used to try to convert
		// filtering on the id field to a __key__ query, but that tended to confuse users about the real capabilities
		// of GAE and Objectify. So let's force users to use filterKey() instead.
		if (this.classRestriction != null) {
			KeyMetadata<?> meta = loader.ofy.factory().keys().getMetadataSafe(this.classRestriction);

			if (prop.equals(meta.getParentFieldName())) {
				throw new IllegalArgumentException("@Parent fields cannot be filtered on. Perhaps you wish to use filterKey() or ancestor() instead?");
			}
			else if (prop.equals(meta.getIdFieldName())) {
				throw new IllegalArgumentException("@Id fields cannot be filtered on. Perhaps you wish to use filterKey() instead?");
			}
		}

		// Convert to something filterable, possibly extracting/converting keys
		value = loader.getObjectifyImpl().makeFilterable(value);

		addFilter(op.of(prop, value));

		if (op == FilterOperator.IN || op == FilterOperator.NOT_EQUAL)
			hasMulti = true;
	}

	/**
	 * Add the filter as an AND to whatever is currently set as the actual filter.
	 */
	void addFilter(Filter filter) {
		if (actual.getFilter() == null) {
			actual.setFilter(filter);
		} else {
			actual.setFilter(CompositeFilterOperator.and(actual.getFilter(), filter));
		}
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
			return FilterOperator.NOT_EQUAL;
		else if (operator.toLowerCase().equals("in"))
			return FilterOperator.IN;
		else
			throw new IllegalArgumentException("Unknown operator '" + operator + "'");
	}

	/** Modifies the instance */
	void addOrder(String condition) {
		condition = condition.trim();
		SortDirection dir = SortDirection.ASCENDING;

		if (condition.startsWith("-")) {
			dir = SortDirection.DESCENDING;
			condition = condition.substring(1).trim();
		}

		// Check for @Id or @Parent fields.  Any setting adjusts the key order.  We only enforce that they are both set the same direction.
		if (this.classRestriction != null) {
			KeyMetadata<?> meta = loader.ofy.factory().keys().getMetadataSafe(this.classRestriction);

			if (condition.equals(meta.getParentFieldName()))
				throw new IllegalArgumentException("You cannot order by @Parent field. Perhaps you wish to order by __key__ instead?");

			if (condition.equals(meta.getIdFieldName())) {
				throw new IllegalArgumentException("You cannot order by @Id field. Perhaps you wish to order by __key__ instead?");
			}
		}

		this.actual.addSort(condition, dir);
	}

	/** Modifies the instance */
	void setAncestor(Object keyOrEntity) {
		this.actual.setAncestor(loader.ofy.factory().keys().anythingToRawKey(keyOrEntity));
	}

	/** Modifies the instance */
	void setLimit(int value) {
		this.limit = value;

		if (this.chunk == null)
			this.chunk = value;
	}

	/** Modifies the instance */
	void setOffset(int value) {
		this.offset = value;
	}

	/** Modifies the instance */
	void setStartCursor(Cursor value) {
		this.startAt = value;
	}

	/** Modifies the instance */
	void setEndCursor(Cursor value) {
		this.endAt = value;
	}

	/** Modifies the instance */
	void setChunk(int value) {
		this.chunk = value;
	}

	/** Modifies the instance */
	void setHybrid(boolean force) {
		this.hybrid = force;
		this.hasExplicitHybrid = true;
	}

	/** Modifies the instance */
	void setKeysOnly() {
		if (!this.actual.getProjections().isEmpty())
			throw new IllegalStateException("You cannot ask for both keys-only and projections in the same query. That makes no sense!");

		this.actual.setKeysOnly();
	}

	/** Modifies the instance, switching directions */
	void toggleReverse() {
		this.actual = this.actual.reverse();
	}

	/** Modifies the instance */
	void setDistinct(boolean value) {
		this.actual.setDistinct(value);
	}

	/** Modifies the instance */
	void addProjection(String... fields) {
		if (this.actual.isKeysOnly())
			throw new IllegalStateException("You cannot ask for both keys-only and projections in the same query. That makes no sense!");

		for (String field: fields) {
			this.actual.addProjection(new PropertyProjection(field, null));
		}
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder bld = new StringBuilder(this.getClass().getName());
		bld.append("{kind=");
		bld.append(this.actual.getKind());

		bld.append(",ancestor=");
		if (this.actual.getAncestor() != null)
			bld.append(KeyFactory.keyToString(this.actual.getAncestor()));

		// Filter and sort don't necessarily produce stable values (ordering might be different), but this is not
		// necessarily a fatal problem. Just a potential inefficiency if the query is being used as a cache key.

		if (this.actual.getFilter() != null)
			bld.append(",filter=").append(this.actual.getFilter());

		if (!this.actual.getSortPredicates().isEmpty())
			bld.append(",sort=").append(this.actual.getSortPredicates());

		if (this.limit > 0)
			bld.append(",limit=").append(this.limit);

		if (this.offset > 0)
			bld.append(",offset=").append(this.offset);

		if (this.startAt != null)
			bld.append(",startAt=").append(this.startAt.toWebSafeString());

		if (this.endAt != null)
			bld.append(",endAt=").append(this.endAt.toWebSafeString());

		if (this.actual.getDistinct())
			bld.append(",distinct=true");

		if (!this.actual.getProjections().isEmpty())
			bld.append(",projections=").append(this.actual.getProjections());

		bld.append('}');

		return bld.toString();
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Query#first()
	 */
	@Override
	public LoadResult<T> first() {
		// By the way, this is the same thing that PreparedQuery.asSingleEntity() does internally
		Iterator<T> it = this.limit(1).resultIterable().iterator();

		return new LoadResult<>(null, new IteratorFirstResult<>(it));
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Query#count()
	 */
	@Override
	public int count() {
		return loader.createQueryEngine().queryCount(this.getActualQuery(), this.fetchOptions());
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.QueryExecute#iterable()
	 */
	@Override
	public QueryResultIterable<T> iterable() {
		return resultIterable();
	}

	/* (non-Javadoc)
	 * @see com.google.appengine.api.datastore.QueryResultIterable#iterator()
	 */
	@Override
	public QueryResultIterator<T> iterator() {
		return iterable().iterator();
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Query#list()
	 */
	@Override
	public List<T> list() {
		return ResultProxy.create(List.class, new MakeListResult<>(this.chunk(Integer.MAX_VALUE).iterable()));
	}

	/**
	 * Get an iterator over the keys.  Not part of the public api, but used by QueryKeysImpl.  Assumes
	 * that setKeysOnly() has already been set.
	 */
	public QueryResultIterable<Key<T>> keysIterable() {
		assert actual.isKeysOnly();
		return loader.createQueryEngine().queryKeysOnly(this.getActualQuery(), this.fetchOptions());
	}

	/** Produces the basic iterable on results based on the current query.  Used to generate other iterables via transformation. */
	private QueryResultIterable<T> resultIterable() {
		boolean hybridize = hybrid;

		if (!hasExplicitHybrid) {
			// These are the special conditions we know about.  It may expand.

			if (hasMulti)
				hybridize = false;
		}

		if (hybridize)
			return loader.createQueryEngine().queryHybrid(this.getActualQuery(), this.fetchOptions());
		else
			return loader.createQueryEngine().queryNormal(this.getActualQuery(), this.fetchOptions());
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#clone()
	 */
	@SuppressWarnings({"unchecked", "CloneDoesntDeclareCloneNotSupportedException"})
	public QueryImpl<T> clone() {
		try {
			QueryImpl<T> impl = (QueryImpl<T>)super.clone();
			impl.actual = DatastoreUtils.cloneQuery(this.actual);
			return impl;
		}
		catch (CloneNotSupportedException e) {
			// impossible
			throw new RuntimeException(e);
		}
	}

	/**
	 * @return a set of fetch options for the current limit, offset, and cursors,
	 *  based on the default fetch options.  There will always be options even if default.
	 */
	private FetchOptions fetchOptions() {
		FetchOptions opts = FetchOptions.Builder.withDefaults();

		if (this.startAt != null)
			opts = opts.startCursor(this.startAt);

		if (this.endAt != null)
			opts = opts.endCursor(this.endAt);

		if (this.limit != 0)
			opts = opts.limit(this.limit);

		if (this.offset != 0)
			opts = opts.offset(this.offset);

		if (this.chunk == null)
			opts = opts.chunkSize(DEFAULT_CHUNK_SIZE);
		else
			opts = opts.chunkSize(this.chunk);

		return opts;
	}

	/** Convenience method */
	private ObjectifyFactory fact() {
		return loader.getObjectify().factory();
	}
}
