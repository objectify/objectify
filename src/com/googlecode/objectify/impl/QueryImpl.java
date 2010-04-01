package com.googlecode.objectify.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.QueryResultIterable;
import com.google.appengine.api.datastore.QueryResultIterator;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.appengine.api.datastore.Query.SortDirection;
import com.google.appengine.api.datastore.Query.SortPredicate;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.Query;

/**
 * Implementation of Query.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class QueryImpl<T> implements Query<T>
{
	/** */
	ObjectifyFactory factory;
	Objectify ofy;
	
	/** We need to track this because it enables the ability to filter/sort by id */
	Class<T> classRestriction;
	
	/** The actual datastore query constructed by this object */
	com.google.appengine.api.datastore.Query actual;
	
	/** */
	int limit;
	int offset;
	Cursor cursor;
	
	/** */
	public QueryImpl(ObjectifyFactory fact, Objectify objectify) 
	{
		this.factory = fact;
		this.ofy = objectify;
		this.actual = new com.google.appengine.api.datastore.Query();
	}
	
	/** */
	public QueryImpl(ObjectifyFactory fact, Objectify objectify, Class<T> clazz)
	{
		this.factory = fact;
		this.ofy = objectify;
		this.actual = new com.google.appengine.api.datastore.Query(this.factory.getKind(clazz));
		
		this.classRestriction = clazz;
	}
	
	/** @return the underlying datastore query object */
	protected com.google.appengine.api.datastore.Query getActual()
	{
		return this.actual;
	}
	
	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Query#filter(java.lang.String, java.lang.Object)
	 */
	@Override
	public Query<T> filter(String condition, Object value)
	{
		String[] parts = condition.trim().split(" ");
		if (parts.length < 1 || parts.length > 2)
			throw new IllegalArgumentException("'" + condition + "' is not a legal filter condition");
		
		String prop = parts[0].trim();
		FilterOperator op = (parts.length == 2) ? this.translate(parts[1]) : FilterOperator.EQUAL;

		// If we have a class restriction, check to see if the property is the @Id
		if (this.classRestriction != null)
		{
			EntityMetadata<?> meta = this.factory.getMetadata(this.classRestriction);
			if (meta.isIdField(prop) || meta.isNameField(prop))
			{
				if (meta.hasParentField())
					throw new IllegalStateException("Cannot (yet) filter by @Id fields on entities which have @Parent fields. Tried '" + prop + "' on " + this.classRestriction.getName() + ".");
				
				if (meta.isIdField(prop))
					value = KeyFactory.createKey(meta.getKind(), ((Number)value).longValue());
				else
					value = KeyFactory.createKey(meta.getKind(), value.toString());
				
				prop = "__key__";
			}
		}

		// Convert to something filterable, possibly extracting/converting keys
		value = this.factory.makeFilterable(value);
		
		this.actual.addFilter(prop, op, value);
		
		return this;
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
	
	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Query#order(java.lang.String)
	 */
	@Override
	public Query<T> order(String condition)
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
			EntityMetadata<?> meta = this.factory.getMetadata(this.classRestriction);
			if (meta.isIdField(condition) || meta.isNameField(condition))
				condition = "__key__";
		}

		this.actual.addSort(condition, dir);
		
		return this;
	}
	
	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Query#ancestor(java.lang.Object)
	 */
	@Override
	public Query<T> ancestor(Object keyOrEntity)
	{
		this.actual.setAncestor(this.factory.getRawKey(keyOrEntity));
		return this;
	}
	
	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Query#limit(int)
	 */
	@Override
	public Query<T> limit(int value)
	{
		this.limit = value;
		return this;
	}
	
	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Query#offset(int)
	 */
	@Override
	public Query<T> offset(int value)
	{
		this.offset = value;
		return this;
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Query#cursor(com.google.appengine.api.datastore.Cursor)
	 */
	@Override
	public Query<T> cursor(Cursor value)
	{
		this.cursor = value;
		return this;
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
		
		if (this.cursor != null)
			bld.append(",cursor=").append(this.cursor.toWebSafeString());

		bld.append('}');
		
		return bld.toString();
	}

	/* (non-Javadoc)
	 * @see java.lang.Iterable#iterator()
	 */
	@Override
	public QueryResultIterator<T> iterator()
	{
		FetchOptions opts = this.fetchOptions();
		if (opts == null)
			return new ToObjectIterator<T>(this.prepare().asQueryResultIterator(), false);
		else
			return new ToObjectIterator<T>(this.prepare().asQueryResultIterator(opts), false);
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Query#get()
	 */
	@Override
	public T get()
	{
		// The underlying datastore is basically doing this for PreparedQuery.asSingleEntity(),
		// so let's do it ourselves and integrate offset()

		FetchOptions opts = FetchOptions.Builder.withLimit(1);
		if (this.offset > 0)
			opts = opts.offset(this.offset);
		if (this.cursor != null)
			opts = opts.cursor(this.cursor);
		
		Iterator<Entity> it = this.prepare().asIterator(opts);

		if (it.hasNext())
		{
			Entity ent = it.next();
			EntityMetadata<T> metadata = this.factory.getMetadata(ent.getKey());
			return metadata.toObject(ent);
		}
		else
		{
			return null;
		}
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Query#getKey()
	 */
	@Override
	public Key<T> getKey()
	{
		FetchOptions opts = FetchOptions.Builder.withLimit(1);
		if (this.offset > 0)
			opts = opts.offset(this.offset);
		
		Iterator<Entity> it = this.prepareKeysOnly().asIterator(opts);

		if (it.hasNext())
		{
			Entity ent = it.next();
			return this.factory.rawKeyToTypedKey(ent.getKey());
		}
		else
		{
			return null;
		}
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Query#countAll()
	 */
	@Override
	public int countAll()
	{
		return this.prepare().countEntities();
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Query#fetch()
	 */
	@Override
	public QueryResultIterable<T> fetch()
	{
		FetchOptions opts = this.fetchOptions();
		if (opts == null)
			return this;	// We are already iterable
		else
			return new ToObjectIterable<T>(this.prepare().asQueryResultIterable(opts), false);
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Query#fetchKeys()
	 */
	@Override
	public QueryResultIterable<Key<T>> fetchKeys()
	{
		FetchOptions opts = this.fetchOptions();
		if (opts == null)
			return new ToObjectIterable<Key<T>>(this.prepareKeysOnly().asQueryResultIterable(), true);
		else
			return new ToObjectIterable<Key<T>>(this.prepareKeysOnly().asQueryResultIterable(opts), true);
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Query#fetchParentKeys()
	 */
	@Override
	public <V> Set<Key<V>> fetchParentKeys()
	{
		Set<Key<V>> parentKeys = new LinkedHashSet<Key<V>>();
		
		for (Key<T> key: this.fetchKeys())
		{
			if (key.getParent() == null)
				throw new IllegalStateException("Tried to fetch parent from a key that has no parent: " + key);
			
			parentKeys.add(key.<V>getParent());
		}
		
		return parentKeys;
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Query#fetchParents()
	 */
	@Override
	public <V> Map<Key<V>, V> fetchParents()
	{
		Set<Key<V>> parentKeys = this.fetchParentKeys();
		return this.ofy.get(parentKeys);
	}


	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Query#list()
	 */
	@Override
	public List<T> list()
	{
		List<T> result = new ArrayList<T>();
		for (T obj: this)
			result.add(obj);
		
		return result;
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Query#listKeys()
	 */
	@Override
	public List<Key<T>> listKeys()
	{
		List<Key<T>> result = new ArrayList<Key<T>>();
		for (Key<T> key: this.fetchKeys())
			result.add(key);
		
		return result;
	}

	/**
	 * Create a PreparedQuery relevant to our current state.
	 */
	private PreparedQuery prepare()
	{
		return this.ofy.getDatastore().prepare(this.ofy.getTxn(), this.actual);
	}

	/**
	 * Create a PreparedQuery that fetches keys only, relevant to our current state.
	 */
	private PreparedQuery prepareKeysOnly()
	{
		// Can't modify the query, we might need to use it again
		com.google.appengine.api.datastore.Query cloned = this.cloneRawQuery(this.actual);
		cloned.setKeysOnly();
		
		return this.ofy.getDatastore().prepare(this.ofy.getTxn(), cloned);
	}
	
	/**
	 * @return a set of fetch options for the current limit and offset, or null if
	 *  there is no limit or offset.
	 */
	private FetchOptions fetchOptions()
	{
		FetchOptions opts = null;
		
		if (this.cursor != null)
		{
			opts = FetchOptions.Builder.withCursor(this.cursor);
		}
		
		if (this.limit != 0)
		{
			if (opts == null)
				opts = FetchOptions.Builder.withLimit(this.limit);
			else
				opts = opts.limit(this.limit);
		}
		
		if (this.offset != 0)
		{
			if (opts == null)
				opts = FetchOptions.Builder.withOffset(this.offset);
			else
				opts = opts.offset(this.offset);
		}

		return opts;
	}
	
	/**
	 * Make a new Query object that is exactly like the old.  Too bad Query isn't Cloneable. 
	 */
	protected com.google.appengine.api.datastore.Query cloneRawQuery(com.google.appengine.api.datastore.Query orig)
	{
		com.google.appengine.api.datastore.Query copy = new com.google.appengine.api.datastore.Query(orig.getKind(), orig.getAncestor());
		
		for (FilterPredicate filter: orig.getFilterPredicates())
			copy.addFilter(filter.getPropertyName(), filter.getOperator(), filter.getValue());
		
		for (SortPredicate sort: orig.getSortPredicates())
			copy.addSort(sort.getPropertyName(), sort.getDirection());
		
		// This should be impossible but who knows what might happen in the future
		if (orig.isKeysOnly())
			copy.setKeysOnly();
		
		return copy;
	}
	
	/**
	 * Iterable that translates from datastore Entity to types Objects
	 */
	class ToObjectIterable<S> implements QueryResultIterable<S>
	{
		QueryResultIterable<Entity> source;
		boolean keysOnly;

		public ToObjectIterable(QueryResultIterable<Entity> source, boolean keysOnly)
		{
			this.source = source;
			this.keysOnly = keysOnly;
		}

		@Override
		public QueryResultIterator<S> iterator()
		{
			return new ToObjectIterator<S>(this.source.iterator(), this.keysOnly);
		}
	}

	/**
	 * Iterator that translates from datastore Entity to typed Objects
	 */
	class ToObjectIterator<S> implements QueryResultIterator<S>
	{
		QueryResultIterator<Entity> source;
		boolean keysOnly;

		public ToObjectIterator(QueryResultIterator<Entity> source, boolean keysOnly)
		{
			this.source = source;
			this.keysOnly = keysOnly;
		}

		@Override
		public boolean hasNext()
		{
			return this.source.hasNext();
		}

		@Override
		@SuppressWarnings("unchecked")
		public S next()
		{
			Entity nextEntity = this.source.next();
			if (keysOnly)
			{
				// This will be a ToObjectIterator<Key<T>>
				return (S)factory.rawKeyToTypedKey(nextEntity.getKey());
			}
			else
			{
				EntityMetadata<S> meta = factory.getMetadata(nextEntity.getKey());
				return meta.toObject(nextEntity);
			}
		}

		@Override
		public void remove()
		{
			this.source.remove();
		}

		@Override
		public Cursor getCursor()
		{
			return this.source.getCursor();
		}
	}
}
