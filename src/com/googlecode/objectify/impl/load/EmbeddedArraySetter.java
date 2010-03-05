package com.googlecode.objectify.impl.load;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.AbstractList;
import java.util.Arrays;
import java.util.Collection;

import com.googlecode.objectify.impl.TypeUtils;

/**
 * <p>This setter handles an embedded array by constructing the array and all the objects
 * inside.  It expects the value to be a collection type so it "fans out" the values to
 * all the embedded objects in the array, calling the next setter in the chain with each
 * value.</p>
 */
public class EmbeddedArraySetter extends EmbeddedMultivalueSetter
{
	Class<?> componentType;
	Constructor<?> componentTypeCtor;
	
	/**
	 * The lame Arrays.asList() wrapper doesn't support the clear() operation, and
	 * we need to be able to set the size back to 0 for our newly crated array of
	 * nulls.  This wrapper uses the array as a backing store but it doesn't start
	 * out thinking the collection is populated.
	 */
	static class EmptyArrayListWrapper extends AbstractList<Object>
	{
		int size;
		Object[] array;
		
		public EmptyArrayListWrapper(Object[] array)
		{
			this.array = array;
		}

		@Override
		public Object get(int index)
		{
			return this.array[index];
		}

		@Override
		public int size()
		{
			return this.size;
		}
		
		@Override
		public boolean add(Object element)
		{
			this.array[this.size++] = element;
			return true;
		}
	}

	/** */
	public EmbeddedArraySetter(Field field, String path, Collection<String> collisionPaths)
	{
		super(field, path, collisionPaths);
		assert field.getType().isArray();
		
		this.componentType = this.field.getType().getComponentType();
		this.componentTypeCtor = TypeUtils.getNoArgConstructor(this.componentType);
	}
	
	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.load.EmbeddedMultivalueSetter#getComponentConstructor()
	 */
	@Override
	protected Constructor<?> getComponentConstructor()
	{
		return this.componentTypeCtor;
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.load.EmbeddedMultivalueSetter#getOrCreateCollection(java.lang.Object, int)
	 */
	@Override
	protected Collection<Object> getOrCreateCollection(Object onPojo, int size)
	{
		// First we need the actual array of child objects
		Object embeddedArray = this.field.get(onPojo);
		if (embeddedArray == null)
		{
			// Make the array and set it on the pojo field
			embeddedArray = Array.newInstance(this.componentType, size);
			this.field.set(onPojo, embeddedArray);
			
			return new EmptyArrayListWrapper((Object[])embeddedArray);
		}
		else
		{
			return Arrays.asList((Object[])embeddedArray);
		}
		
	}
}
