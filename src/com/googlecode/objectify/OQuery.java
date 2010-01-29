package com.googlecode.objectify;

import java.util.Arrays;
import java.util.Comparator;

import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.appengine.api.datastore.Query.SortDirection;
import com.google.appengine.api.datastore.Query.SortPredicate;



/**
 * <p>This is similar to the datastore Query object, but better understands
 * real class objects - it allows you to filter and sort by the key field
 * normally.</p>
 * 
 * <p>The methods of this class follow the GAE/Python Query class rather than
 * the GAE/Java Query class because the Python version is much more convenient
 * to use.  The Java version seems to have been designed for machines, not
 * humans.  You will appreciate the improvement.</p>
 * 
 * <p>Construct this class by calling {@code ObjectifyFactory.createQuery()}</p>
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class OQuery<T>
{
	/** We need this for lookups */
	ObjectifyFactory factory;
	
	/** We need to track this because it enables the ability to filter/sort by id */
	Class<T> classRestriction;
	
	/** The actual datastore query constructed by this object */
	protected com.google.appengine.api.datastore.Query actual;
	
	/** */
	protected OQuery(ObjectifyFactory fact) 
	{
		this.factory = fact;
		this.actual = new com.google.appengine.api.datastore.Query();
	}
	
	/** */
	protected OQuery(ObjectifyFactory fact, Class<T> clazz)
	{
		this.factory = fact;
		this.actual = new com.google.appengine.api.datastore.Query(this.factory.getKind(clazz));
		
		this.classRestriction = clazz;
	}
	
	/** @return the underlying datastore query object */
	protected com.google.appengine.api.datastore.Query getActual()
	{
		return this.actual;
	}
	
	/**
	 * <p>Create a filter based on the specified condition and value, using
	 * the same syntax as the Python query class. Examples:</p>
	 * 
	 * <ul>
	 * <li>{@code filter("age >=", age)}</li>
	 * <li>{@code filter("age =", age)}</li>
	 * <li>{@code filter("age", age)} (if no operator, = is assumed)</li>
	 * <li>{@code filter("age !=", age)}</li>
	 * <li>{@code filter("age in", ageList)}</li>
	 * </ul>
	 * 
	 * <p>You can filter on id properties <strong>if</strong> this query is
	 * restricted to a Class<T> and the entity has no @Parent.  If you are
	 * having trouble working around this limitation, please consult the
	 * objectify-appengine google group.</p>
	 * <p>You can <strong>not</strong> filter on @Parent properties.  Use
	 * the {@code ancestor()} method instead.</p>
	 */
	public OQuery<T> filter(String condition, Object value)
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

		if (value instanceof Key<?>)
			value = this.factory.oKeyToRawKey((Key<?>)value);
		
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
	
	/**
	 * <p>Sorts based on a property.  Examples:</p>
	 * 
	 * <ul>
	 * <li>{@code sort("age")}</li>
	 * <li>{@code sort("-age")} (descending sort)</li>
	 * </ul>
	 * 
	 * <p>You can sort on id properties <strong>if</strong> this query is
	 * restricted to a Class<T>.  Note that this is only important for
	 * descending sorting; default iteration is key-ascending.</p>
	 * <p>You can <strong>not</strong> sort on @Parent properties.</p>
	 */
	public OQuery<T> sort(String condition)
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
	
	/**
	 * Restricts result set only to objects which have the given ancestor
	 * somewhere in the chain.  Doesn't need to be the immediate parent.
	 * 
	 * @param keyOrEntity can be an Key, a Key, or an Objectify entity object.
	 */
	public OQuery<T> ancestor(Object keyOrEntity)
	{
		this.actual.setAncestor(this.factory.getRawKey(keyOrEntity));
		return this;
	}
	
	/**
	 * <p>Generates a string that consistently and uniquely specifies this query.  There
	 * is no way to convert this string back into a query and there is no guarantee that
	 * the string will be consistent across versions of Objectify.</p>
	 * 
	 * <p>In particular, this value is useful as a key for a simple memcache query cache.</p> 
	 */
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
		
		bld.append('}');
		
		return bld.toString();
	}
}