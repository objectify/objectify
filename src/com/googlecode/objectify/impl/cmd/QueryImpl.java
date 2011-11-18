package com.googlecode.objectify.impl.cmd;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.appengine.api.datastore.Query.SortDirection;
import com.google.appengine.api.datastore.Query.SortPredicate;
import com.google.appengine.api.datastore.QueryResultIterable;
import com.google.appengine.api.datastore.QueryResultIterator;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.Result;
import com.googlecode.objectify.annotation.Subclass;
import com.googlecode.objectify.cmd.Query;
import com.googlecode.objectify.impl.EntityMetadata;
import com.googlecode.objectify.impl.PolymorphicEntityMetadata;
import com.googlecode.objectify.impl.QueryRef;
import com.googlecode.objectify.util.DatastoreUtils;
import com.googlecode.objectify.util.ResultProxy;
import com.googlecode.objectify.util.ResultTranslator;

/**
 * Implementation of Query.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
class QueryImpl<T> extends QueryDefinition<T> implements Query<T>, Cloneable
{
	/** We need to track this because it enables the ability to filter/sort by id */
	Class<T> classRestriction;
	
	/** The actual datastore query constructed by this object */
	com.google.appengine.api.datastore.Query actual;
	
	/** */
	int limit;
	int offset;
	Cursor startAt;
	Cursor endAt;
	Integer chunkSize;
	Integer prefetchSize;
	
	/** */
	QueryImpl(ObjectifyImpl objectify, Set<String> fetchGroups) {
		super(objectify, fetchGroups);
		this.actual = new com.google.appengine.api.datastore.Query();
	}
	
	/** */
	QueryImpl(ObjectifyImpl objectify, Set<String> fetchGroups, Class<T> clazz) {
		super(objectify, fetchGroups);
		
		this.actual = new com.google.appengine.api.datastore.Query(Key.getKind(clazz));
		
		// If this is a polymorphic subclass, add an extra filter
		Subclass sub = clazz.getAnnotation(Subclass.class);
		if (sub != null)
		{
			String discriminator = sub.name().length() > 0 ? sub.name() : clazz.getSimpleName();
			this.actual.addFilter(PolymorphicEntityMetadata.DISCRIMINATOR_INDEX_PROPERTY, FilterOperator.EQUAL, discriminator);
		}
		
		this.classRestriction = clazz;
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.cmd.QueryBase#createQuery()
	 */
	@Override
	QueryImpl<T> createQuery() {
		return this.clone();
	}

	/** @return the underlying datastore query object */
	protected com.google.appengine.api.datastore.Query getActual() {
		return this.actual;
	}
	
	/** Modifies the instance */
	void addFilter(String condition, Object value)
	{
		String[] parts = condition.trim().split(" ");
		if (parts.length < 1 || parts.length > 2)
			throw new IllegalArgumentException("'" + condition + "' is not a legal filter condition");
		
		String prop = parts[0].trim();
		FilterOperator op = (parts.length == 2) ? this.translate(parts[1]) : FilterOperator.EQUAL;

		// If we have a class restriction, check to see if the property is the @Id
		if (this.classRestriction != null)
		{
			EntityMetadata<?> meta = ofy.getFactory().getMetadata(this.classRestriction);
			if (meta.isIdField(prop) || meta.isNameField(prop))
			{
				if (meta.hasParentField())
					throw new IllegalStateException("Cannot (yet) filter by @Id fields on entities which have @Parent fields. Tried '" + prop + "' on " + this.classRestriction.getName() + ".");

				boolean isNumericId = meta.isIdField(prop);
				
				if (op == FilterOperator.IN)
				{
					if (!(value instanceof Iterable<?> || value instanceof Object[]))
						throw new IllegalStateException("IN operator requires a collection value.  Value was " + value);

					if (value instanceof Object[])
						value = Arrays.asList(((Object[])value));
					
					// This is a bit complicated - we need to make a list of vanilla datastore Key objects.
					
					List<Object> keys = (value instanceof Collection<?>)
						? new ArrayList<Object>(((Collection<?>)value).size())
						: new ArrayList<Object>();
						
					for (Object obj: (Iterable<?>)value)
					{
						if (isNumericId)
							keys.add(KeyFactory.createKey(meta.getKind(), ((Number)obj).longValue()));
						else
							keys.add(KeyFactory.createKey(meta.getKind(), obj.toString()));
					}
					
					value = keys;
				}
				else
				{
					if (isNumericId)
						value = KeyFactory.createKey(meta.getKind(), ((Number)value).longValue());
					else
						value = KeyFactory.createKey(meta.getKind(), value.toString());
				}
				
				prop = "__key__";
			}
		}

		// Convert to something filterable, possibly extracting/converting keys
		value = ofy.getFactory().makeFilterable(value);
		
		this.actual.addFilter(prop, op, value);
	}
	
	/**
	 * Converts the textual operator (">", "<=", etc) into a FilterOperator.
	 * Forgiving about the syntax; != and <> are NOT_EQUAL, = and == are EQUAL.
	 */
	protected FilterOperator translate(String operator)
	{
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
	void addOrder(String condition)
	{
		condition = condition.trim();
		SortDirection dir = SortDirection.ASCENDING;
		
		if (condition.startsWith("-"))
		{
			dir = SortDirection.DESCENDING;
			condition = condition.substring(1).trim();
		}
		
		// Check for @Id field
		if (this.classRestriction != null)
		{
			EntityMetadata<?> meta = ofy.getFactory().getMetadata(this.classRestriction);
			if (meta.isIdField(condition) || meta.isNameField(condition))
				condition = "__key__";
		}

		this.actual.addSort(condition, dir);
	}
	
	/** Modifies the instance */
	void setAncestor(Object keyOrEntity) {
		this.actual.setAncestor(ofy.getFactory().getRawKey(keyOrEntity));
	}
	
	/** Modifies the instance */
	void setLimit(int value) {
		this.limit = value;
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
	void setChunkSize(int value) {
		this.chunkSize = value;
	}

	/** Modifies the instance */
	void setPrefetchSize(int value) {
		this.prefetchSize = value;
	}

	/** Modifies the instance */
	void setKeysOnly() {
		this.actual.setKeysOnly();
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		StringBuilder bld = new StringBuilder(this.getClass().getName());
		bld.append("{kind=");
		bld.append(this.actual.getKind());
		
		bld.append(",ancestor=");
		if (this.actual.getAncestor() != null)
			bld.append(KeyFactory.keyToString(this.actual.getAncestor()));

		// We need to sort filters to make a stable string value
		FilterPredicate[] filters = this.actual.getFilterPredicates().toArray(new FilterPredicate[this.actual.getFilterPredicates().size()]);
		Arrays.sort(filters, new Comparator<FilterPredicate>() {
			@Override
			public int compare(FilterPredicate o1, FilterPredicate o2)
			{
				int result = o1.getPropertyName().compareTo(o2.getPropertyName());
				if (result != 0)
					return result;
				
				result = o1.getOperator().compareTo(o2.getOperator());
				if (result != 0)
					return result;
				
				if (o1.getValue() == null)
					return o2.getValue() == null ? 0 : -1;
				else if (o2.getValue() == null)
					return 1;
				else
					return o1.getValue().toString().compareTo(o2.getValue().toString());	// not perfect, but probably as good as we can do
			}
		});
		for (FilterPredicate filter: filters)
		{
			bld.append(",filter=");
			bld.append(filter.getPropertyName());
			bld.append(filter.getOperator().name());
			bld.append(filter.getValue());
		}
		
		// We need to sort sorts to make a stable string value
		SortPredicate[] sorts = this.actual.getSortPredicates().toArray(new SortPredicate[this.actual.getSortPredicates().size()]);
		Arrays.sort(sorts, new Comparator<SortPredicate>() {
			@Override
			public int compare(SortPredicate o1, SortPredicate o2)
			{
				int result = o1.getPropertyName().compareTo(o2.getPropertyName());
				if (result != 0)
					return result;

				// Actually, it should be impossible to have the same prop with multiple directions
				return o1.getDirection().compareTo(o2.getDirection());
			}
		});
		for (SortPredicate sort: this.actual.getSortPredicates())
		{
			bld.append(",sort=");
			bld.append(sort.getPropertyName());
			bld.append(sort.getDirection().name());
		}
		
		if (this.limit > 0)
			bld.append(",limit=").append(this.limit);
		
		if (this.offset > 0)
			bld.append(",offset=").append(this.offset);
		
		if (this.startAt != null)
			bld.append(",startAt=").append(this.startAt.toWebSafeString());

		if (this.endAt != null)
			bld.append(",endAt=").append(this.endAt.toWebSafeString());

		bld.append('}');
		
		return bld.toString();
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Query#first()
	 */
	@Override
	public Ref<T> first() {
		// The underlying datastore is basically doing this for PreparedQuery.asSingleEntity(),
		// so we can do it by faking the limit
		
		int oldLimit = this.limit;
		try {
			this.limit = 1;
			Iterator<T> it = ofy.getEngine().<T>query(actual, fetchOptions()).iterator();
			
			Result<T> result = new ResultTranslator<Iterator<T>, T>(it) {
				@Override
				protected T translate(Iterator<T> from) {
					return from.hasNext() ? from.next() : null;
				}
			};
			
			return new QueryRef<T>(result, ofy);
					
		} finally {
			this.limit = oldLimit;
		}
		
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Query#count()
	 */
	@Override
	public int count() {
		return ofy.getEngine().queryCount(actual, this.fetchOptions());
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Query#fetch()
	 */
	@Override
	public QueryResultIterable<T> entities() {
		return ofy.getEngine().query(actual, this.fetchOptions());
	}

	/* (non-Javadoc)
	 * @see com.google.appengine.api.datastore.QueryResultIterable#iterator()
	 */
	@Override
	public QueryResultIterator<T> iterator() {
		return entities().iterator();
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Query#keys()
	 */
	@Override
	public QueryResultIterable<Key<T>> keys()
	{
		// Can't modify the query, we might need to use it again
		com.google.appengine.api.datastore.Query cloned = DatastoreUtils.cloneQuery(this.actual);
		cloned.setKeysOnly();
		
		return ofy.getEngine().queryKeys(cloned, this.fetchOptions());
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Query#list()
	 */
	@Override
	public List<T> list()
	{
		Iterable<T> it = this.entities();
		return makeAsyncList(it);
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Query#listKeys()
	 */
	@Override
	public List<Key<T>> listKeys()
	{
		Iterable<Key<T>> it = this.keys();
		return makeAsyncList(it);
	}
	
	/** Converts an Iterable into a list asynchronously */
	private <S> List<S> makeAsyncList(Iterable<S> it)
	{
		Result<List<S>> result = new ResultTranslator<Iterable<S>, List<S>>(it) {
			@Override
			protected List<S> translate(Iterable<S> from) {
				List<S> list = new ArrayList<S>();
				for (S s: from)
					list.add(s);
				
				return list;
			}
		};
		
		return ResultProxy.create(List.class, result);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#clone()
	 */
	@SuppressWarnings("unchecked")
	public QueryImpl<T> clone()
	{
		try
		{
			QueryImpl<T> impl = (QueryImpl<T>)super.clone();
			impl.actual = DatastoreUtils.cloneQuery(this.actual);
			return impl;
		}
		catch (CloneNotSupportedException e)
		{
			// impossible
			throw new RuntimeException(e);
		}
	}

	/**
	 * @return a set of fetch options for the current limit, offset, and cursors,
	 *  based on the default fetch options.  There will always be options even if default.
	 */
	private FetchOptions fetchOptions()
	{
		FetchOptions opts = FetchOptions.Builder.withDefaults();
		
		if (this.startAt != null)
			opts = opts.startCursor(this.startAt);
		
		if (this.endAt != null)
			opts = opts.endCursor(this.endAt);
		
		if (this.limit != 0)
			opts = opts.limit(this.limit);
		
		if (this.offset != 0)
			opts = opts.offset(this.offset);
		
		if (this.prefetchSize != null)
			opts = opts.prefetchSize(this.prefetchSize);

		if (this.chunkSize != null)
			opts = opts.chunkSize(this.chunkSize);

		return opts;
	}
}
