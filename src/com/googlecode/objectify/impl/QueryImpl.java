package com.googlecode.objectify.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.appengine.api.datastore.Query.SortDirection;
import com.google.appengine.api.datastore.Query.SortPredicate;
import com.google.appengine.api.datastore.QueryResultIterable;
import com.google.appengine.api.datastore.QueryResultIterator;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.Query;
import com.googlecode.objectify.annotation.Subclass;
import com.googlecode.objectify.util.TranslatingQueryResultIterator;

/**
 * Implementation of Query.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class QueryImpl<T> implements Query<T>, Cloneable
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
	Cursor startCursor;
	Cursor endCursor;
	Integer chunkSize;
	Integer prefetchSize;
	
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
	 * @see com.googlecode.objectify.Query#startCursor(com.google.appengine.api.datastore.Cursor)
	 */
	@Override
	public Query<T> startCursor(Cursor value)
	{
		this.startCursor = value;
		return this;
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Query#endCursor(com.google.appengine.api.datastore.Cursor)
	 */
	@Override
	public Query<T> endCursor(Cursor value)
	{
		this.endCursor = value;
		return this;
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Query#chunkSize(int)
	 */
	@Override
	public Query<T> chunkSize(int value)
	{
		this.chunkSize = value;
		return this;
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Query#prefetchSize(int)
	 */
	@Override
	public Query<T> prefetchSize(int value)
	{
		this.prefetchSize = value;
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
		
		if (this.startCursor != null)
			bld.append(",startCursor=").append(this.startCursor.toWebSafeString());

		if (this.endCursor != null)
			bld.append(",endCursor=").append(this.endCursor.toWebSafeString());

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
		return new ToObjectIterator(this.prepare().asQueryResultIterator(opts));
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Query#get()
	 */
	@Override
	public T get()
	{
		// The underlying datastore is basically doing this for PreparedQuery.asSingleEntity(),
		// so we can do it by faking the limit
		
		int oldLimit = this.limit;
		try
		{
			this.limit = 1;
			Iterator<T> it = this.iterator();
			
			T result = null;
			
			if (it.hasNext())
				result = it.next();
			
			return result;
		}
		finally
		{
			this.limit = oldLimit;
		}
		
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Query#getKey()
	 */
	@Override
	public Key<T> getKey()
	{
		int oldLimit = this.limit;
		try
		{
			this.limit = 1;
			Iterator<Key<T>> it = this.fetchKeys().iterator();
			
			Key<T> result = null;
			
			if (it.hasNext())
				result = it.next();
			
			return result;
		}
		finally
		{
			this.limit = oldLimit;
		}
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Query#count()
	 */
	@Override
	public int count()
	{
		return this.prepare().countEntities(this.fetchOptions());
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Query#fetch()
	 */
	@Override
	public QueryResultIterable<T> fetch()
	{
		FetchOptions opts = this.fetchOptions();
		return new ToObjectIterable(this.prepare().asQueryResultIterable(opts));
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Query#fetchKeys()
	 */
	@Override
	public QueryResultIterable<Key<T>> fetchKeys()
	{
		FetchOptions opts = this.fetchOptions();
		return new ToKeyIterable(this.prepareKeysOnly().asQueryResultIterable(opts));
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
	
	/* (non-Javadoc)
	 * @see java.lang.Object#clone()
	 */
	@SuppressWarnings("unchecked")
	public Query<T> clone()
	{
		try
		{
			QueryImpl<T> impl = (QueryImpl<T>)super.clone();
			impl.actual = this.cloneRawQuery(this.actual);
			return impl;
		}
		catch (CloneNotSupportedException e)
		{
			// impossible
			throw new RuntimeException(e);
		}
	}

	/**
	 * Create a PreparedQuery relevant to our current state.
	 */
	private PreparedQuery prepare()
	{
		return this.ofy.async().getAsyncDatastore().prepare(this.ofy.getTxn(), this.actual);
	}

	/**
	 * Create a PreparedQuery that fetches keys only, relevant to our current state.
	 */
	private PreparedQuery prepareKeysOnly()
	{
		// Can't modify the query, we might need to use it again
		com.google.appengine.api.datastore.Query cloned = this.cloneRawQuery(this.actual);
		cloned.setKeysOnly();
		
		return this.ofy.async().getAsyncDatastore().prepare(this.ofy.getTxn(), cloned);
	}
	
	/**
	 * @return a set of fetch options for the current limit, offset, and cursors,
	 *  based on the default fetch options.  There will always be options even if default.
	 */
	private FetchOptions fetchOptions()
	{
		FetchOptions opts = FetchOptions.Builder.withDefaults();
		
		if (this.startCursor != null)
			opts = opts.startCursor(this.startCursor);
		
		if (this.endCursor != null)
			opts = opts.endCursor(this.endCursor);
		
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
	 * Iterable that translates from datastore Entity to Keys
	 */
	protected class ToKeyIterable implements QueryResultIterable<Key<T>>
	{
		QueryResultIterable<Entity> source;

		public ToKeyIterable(QueryResultIterable<Entity> source)
		{
			this.source = source;
		}

		@Override
		public QueryResultIterator<Key<T>> iterator()
		{
			return new ToKeyIterator(this.source.iterator());
		}
	}

	/**
	 * Iterator that translates from datastore Entity to Keys
	 */
	protected class ToKeyIterator extends TranslatingQueryResultIterator<Entity, Key<T>>
	{
		public ToKeyIterator(QueryResultIterator<Entity> source)
		{
			super(source);
		}

		@Override
		protected Key<T> translate(Entity from)
		{
			return new Key<T>(from.getKey());
		}
	}

	/**
	 * Iterable that translates from datastore Entity to POJO
	 */
	protected class ToObjectIterable implements QueryResultIterable<T>
	{
		QueryResultIterable<Entity> source;

		public ToObjectIterable(QueryResultIterable<Entity> source)
		{
			this.source = source;
		}

		@Override
		public QueryResultIterator<T> iterator()
		{
			return new ToObjectIterator(this.source.iterator());
		}
	}

	/**
	 * Iterator that translates from datastore Entity to typed Objects
	 */
	protected class ToObjectIterator extends TranslatingQueryResultIterator<Entity, T>
	{
		public ToObjectIterator(QueryResultIterator<Entity> source)
		{
			super(source);
		}

		@Override
		protected T translate(Entity from)
		{
			EntityMetadata<T> meta = factory.getMetadata(from.getKey());
			return meta.toObject(from, ofy);
		}
	}
}
