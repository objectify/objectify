package com.googlecode.objectify.impl.load;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;

import com.googlecode.objectify.impl.LoadContext;
import com.googlecode.objectify.impl.Transmog;
import com.googlecode.objectify.impl.TypeUtils;
import com.googlecode.objectify.impl.conv.Conversions;

/**
 * Creates objects or stores primitive values within a map embedded in a component.
 */
public class EmbeddedMapSetter extends CollisionDetectingSetter
{

	Field field;
	/** Constructor for the component type, or null if this is a primitive value map. */
	Constructor<?> componentTypeCtor;
	/** Nested Transmog that handles object loading for the component type, or null for primitives. */
	Transmog<Object> nestedTransmog;

	@SuppressWarnings("unchecked")
	public EmbeddedMapSetter(Field field, Class<?> componentType, Conversions conversions,
			Collection<String> collisionPaths)
	{
		super(collisionPaths);
		this.field = field;
		if (Object.class.equals(componentType))
		{
			// If the Map value type is object, we assume a map of primitive datastore values
			this.componentTypeCtor = null;
			this.nestedTransmog = null;
		}
		else
		{
			this.componentTypeCtor = TypeUtils.getConstructor(componentType);
			this.nestedTransmog = new Transmog<Object>(conversions, (Class<Object>) componentType);
		}
	}

	/**
	 * Set the value within our map.
	 */
	@Override
	protected void safeSet(Object toPojo, Object value, LoadContext context)
	{
		@SuppressWarnings("unchecked")
		// we know this is a Map<String, ...> from construction
		Map<String, Object> map = (Map<String, Object>) TypeUtils.field_get(field, toPojo);
		String objectName = context.getMapEntryName();
		String suffix = context.getMapSuffix();
		if (componentTypeCtor != null)
		{
			Object nestedPojo = map.get(objectName);
			if (nestedPojo == null)
			{
				// Not present yet, create a new object
				nestedPojo = TypeUtils.newInstance(componentTypeCtor);
				map.put(objectName, nestedPojo);
			}
			// Delegate to the nested Transmog to set the individual value.
			nestedTransmog.loadSingleValue(suffix, value, nestedPojo, context);
		}
		else
		{
			map.put(objectName, value);
		}
	}
}
