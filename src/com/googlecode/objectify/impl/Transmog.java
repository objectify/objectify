package com.googlecode.objectify.impl;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;

import javax.persistence.Embedded;
import javax.persistence.Id;

import com.google.appengine.api.datastore.Entity;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.annotation.OldName;
import com.googlecode.objectify.annotation.Parent;
import com.googlecode.objectify.annotation.Unindexed;

/**
 * <p>Class which knows how to load data from Entity to POJO and save data from POJO to Entity.</p>
 * <p>Note that this class completely ignores @Id and @Parent fields.</p>
 * <p>A useful thing to remember when trying to understand this class is that in an entity object
 * graph, arrays and collections of basic types are considered leaf nodes.  On the other hand,
 * arrays and collections of @Embedded actually fan out the graph.</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class Transmog<T>
{
	/** Needed to convert Key types */
	ObjectifyFactory factory;
	
	/** Maps full "blah.blah.blah" property name to a particular Setter implementation */
	Map<String, Setter> setters;
	
	/**
	 * Object which visits various levels of the object graph and builds the loaders & savers.
	 * Tracks the current 
	 */
	class Visitor
	{
		Setter setterChain;
		String prefix = "";
		boolean embedded;
		boolean forceUnindexed;
		
		/** Constructs a visitor for a top-level entity */
		public Visitor()
		{
			this.setterChain = new RootSetter();
		}
		
		/**
		 * Constructs a visitor for an embedded object.
		 * @param setterChain is the root of the setter chain
		 */
		public Visitor(Setter setterChain, String prefix, boolean forceUnindexed)
		{
			this.setterChain = setterChain;
			this.prefix = prefix;
			this.embedded = true;
			this.forceUnindexed = forceUnindexed;
		}
		
		/** The money shot */
		public void visitClass(Class<?> clazz)
		{
			if ((clazz == null) || (clazz == Object.class))
				return;

			this.visitClass(clazz.getSuperclass());

			for (Field field: clazz.getDeclaredFields())
				this.visitField(field);

			for (Method method: clazz.getDeclaredMethods())
				this.visitMethod(method);
		}
		
		/**
		 * Check out a method looking for @OldName
		 */
		void visitMethod(Method method)
		{
			OldName oldName = method.getAnnotation(OldName.class);
			if (oldName != null)
			{
				if (method.isAnnotationPresent(Embedded.class))
					throw new IllegalStateException("@Embedded cannot be used on @OldName methods");

				if (method.getParameterTypes().length != 1)
					throw new IllegalStateException("@OldName methods must have a single parameter. Can't use " + method);
				
				method.setAccessible(true);

				Setter setter = new LeafSetter(factory, new MethodWrapper(method));
				this.addSetter(oldName.value(), setter);
			}
		}
		
		/**
		 * Check out a field
		 */
		void visitField(Field field)
		{
			if (!TypeUtils.isSaveable(field)
					|| field.isAnnotationPresent(Id.class)
					|| field.isAnnotationPresent(Parent.class))
				return;

			field.setAccessible(true);
			
			boolean unindexed = this.forceUnindexed || field.isAnnotationPresent(Unindexed.class);
			
			if (field.isAnnotationPresent(Embedded.class))
			{
				if (field.getType().isArray())
				{
					TypeUtils.checkForNoArgConstructor(field.getType().getComponentType());
					
					EmbeddedArraySetter setter = new EmbeddedArraySetter(field);
					Visitor visitor = new Visitor(this.setterChain.extend(setter), this.prefix + field.getName(), unindexed);
					visitor.visitClass(field.getType());
				}
				else if (Collection.class.isAssignableFrom(field.getType()))
				{
					throw new UnsupportedOperationException("@Embedded arrays not supported yet");
				}
				else	// basic class
				{
					TypeUtils.checkForNoArgConstructor(field.getType());

					EmbeddedClassSetter setter = new EmbeddedClassSetter(field);
					Visitor visitor = new Visitor(this.setterChain.extend(setter), this.prefix + field.getName(), unindexed);
					visitor.visitClass(field.getType());
				}
			}
			else	// not embedded, so we're at a leaf object (including arrays of basic types)
			{
				Setter setter = new LeafSetter(factory, new FieldWrapper(field));
				
				this.addSetter(field.getName(), setter);
				
				OldName oldName = field.getAnnotation(OldName.class);
				if (oldName != null)
					this.addSetter(oldName.value(), setter);
			}
		}
		
		/**
		 * Adds a final setter to the setters collection.
		 * @param name is the short, immediate name of the property
		 */
		void addSetter(String name, Setter setter)
		{
			String wholeName = this.prefix + "." + name;
			if (setters.containsKey(wholeName))
				throw new IllegalStateException("Attempting to create multiple associations for " + wholeName);

			// Extend and strip off the unnecessary (at runtime) RootSetter
			Setter chain = this.setterChain.extend(setter).next;
			setters.put(wholeName, chain);
		}
	}
	
	/**
	 * Creats a transmog for the specified class, introspecting it and discovering
	 * how to load/save its properties.
	 */
	public Transmog(ObjectifyFactory fact, Class<T> clazz)
	{
		this.factory = fact;
		new Visitor().visitClass(clazz);
	}
	
	/**
	 * Loads the property data in an Entity into a POJO.  Does not affect id/parent
	 * (ie key) fields; those are assumed to already have been set.
	 * 
	 * @param entity is a raw datastore entity
	 * @param pojo is your typed entity
	 */
	public void load(Entity entity, T pojo)
	{
		for (Map.Entry<String, Object> property: entity.getProperties().entrySet())
		{
			Setter setter = this.setters.get(property.getKey());
			if (setter != null)
				setter.set(pojo, property.getValue());
		}
	}
	
	/**
	 * Saves the fields of a POJO ito the properties of an Entity.  Does not affect id/parent
	 * (ie key) fields; those are assumed to already have been set.
	 * 
	 * @param pojo is your typed entity
	 * @param entity is a raw datastore entity
	 */
	public void save(T pojo, Entity entity)
	{
		
	}
}
