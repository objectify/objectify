package com.googlecode.objectify.impl;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.appengine.api.datastore.Entity;
import com.googlecode.objectify.impl.TypeUtils.FieldMetadata;
import com.googlecode.objectify.impl.TypeUtils.MethodMetadata;
import com.googlecode.objectify.impl.conv.Conversions;
import com.googlecode.objectify.impl.load.EmbeddedArraySetter;
import com.googlecode.objectify.impl.load.EmbeddedClassSetter;
import com.googlecode.objectify.impl.load.EmbeddedCollectionSetter;
import com.googlecode.objectify.impl.load.EmbeddedMapSetter;
import com.googlecode.objectify.impl.load.EmbeddedMultivalueSetter;
import com.googlecode.objectify.impl.load.EmbeddedNullIndexSetter;
import com.googlecode.objectify.impl.load.LeafSetter;
import com.googlecode.objectify.impl.load.RootSetter;
import com.googlecode.objectify.impl.load.Setter;
import com.googlecode.objectify.impl.save.ClassSaver;
import com.googlecode.objectify.impl.save.Path;

/**
 * <p>Class which knows how to load data from Entity to POJO and save data from POJO to Entity.</p>
 * 
 * <p>Note that this class completely ignores @Id and @Parent fields.</p>
 * 
 * <p>To understand this code, you must first understand that a "leaf" value is anything that
 * can be put into the datastore in a single property.  Simple types like String, and Enum,
 * and Key are leaf nodes, but so are Collections and arrays of these basic types.  @Embedded
 * values are nonleaf - they branch the persistance graph, producing multiple properties in a
 * datastore Entity.</p>
 * 
 * <p>Also realize that there are two separate dimensions to understand.  Misunderstanding
 * the two related graphs will make this code very confusing:</p>
 * <ul>
 * <li>There is a class graph, which branches at @Embedded classes (either simple fields
 * or array/collection fields).  The static analysis code that builds Setters and Savers
 * must traverse this graph.</li>
 * <li>There is an object graph, which branches at @Embedded arrays.  The runtime execution
 * code must traverse this graph when setting and saving entities.</li>
 * </ul>
 * 
 * <p>The core structures that operate at runtime are Setters (for loading datastore Entities into
 * typed pojos) and Savers (for saving the fields of typed pojos into datastore Entities).  They are
 * NOT parallel hierarchies, and they work very differently:</p>
 * <ul>
 * <li>When loading, Transmog <em>iterates</em> through the properties of an Entity and for each one calls a Setter
 * that knows how to set this property somewhere deep in the object graph of a typed pojo.  In the case
 * of @Embedded arrays and collections, this single collection datastore value will set multipel
 * values in the pojo.  The core data structure is {@code rootSetters}, a map of entity property
 * name to a Setter which knows what to do with that data.</li>
 * <li>When saving, Transmog <em>recurses</em> through the class structure of a pojo (and any embedded objects), calling
 * all relevant Savers to populate the datastore Entity.  The core data structure is {@code rootSaver}, which
 * understands the whole pojo object graph and knows how to translate it into a number of properties
 * on the Entity.</li>
 * </ul>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class Transmog<T>
{
	/** */
	Conversions conversions;
	
	/** Useful to have around for error logging purposes */
	Class<T> clazz;
	
	/** Maps full "blah.blah.blah" property name to a particular Setter implementation */
	Map<String, Setter> rootSetters = new HashMap<String, Setter>();
	
	/** The root saver that knows how to persist an object of type T */
	ClassSaver rootSaver;
	
	/**
	 * <p>Object which visits various levels of the pojo class graph does two things:
	 * <ol>
	 * <li>Builds up the rootSetters map in {@code rootSetters}.</li>
	 * <li>Validates the structure of the pojo.</li>
	 * </ol>
	 * 
	 * <p>This visitor does not have *anything* to do with Savers, which are built up
	 * separately in a different pass.  This is the inherent nature of the beast.</p>
	 */
	class Visitor
	{
		Setter setterChain;
		String prefix;	// starts null for root
		boolean embedded;
		
		Set<String> fieldPathsUsed;
		Set<String> methodPathsUsed;
		
		/** Constructs a visitor for a top-level entity */
		public Visitor()
		{
			this.setterChain = new RootSetter();
			
			this.fieldPathsUsed = new HashSet<String>();
			this.methodPathsUsed = new HashSet<String>();
		}
		
		/**
		 * Constructs a visitor for an embedded object.
		 * @param setterChain is the root of the setter chain
		 */
		public Visitor(Setter setterChain, String prefix, Set<String> fieldPathsUsed, Set<String> methodPathsUsed)
		{
			this.setterChain = setterChain;
			this.prefix = prefix;
			this.embedded = true;
			
			this.fieldPathsUsed = fieldPathsUsed;
			this.methodPathsUsed = methodPathsUsed;
		}
		
		/**
		 * Creates a set of Setters for the class (and parent/embedded classes) in the
		 * Transmog.rootSetters collection
		 * 
		 * @param currentClazz is the class to inspect
		 */
		public void visitClass(Class<?> currentClazz)
		{
			// Only good fields come back from this method call
			List<FieldMetadata> fields = TypeUtils.getPesistentFields(currentClazz, this.embedded);
			for (FieldMetadata meta: fields)
				this.visitField(meta.field, meta.names);

			// Only good methods come back from this method call
			List<MethodMetadata> methods = TypeUtils.getAlsoLoadMethods(currentClazz);
			for (MethodMetadata meta: methods)
				this.visitMethod(meta.method, meta.names);
		}
		
		/**
		 * @param method must be a proper @AlsoLoad method.
		 * @param names are all the property names which should be used to call the method
		 */
		void visitMethod(Method method, Collection<String> names)
		{
			List<String> paths = this.namesToPaths(names);
			
			for (String path: paths)
			{
				List<String> collisions = makeCollisions(paths, path);
				LeafSetter setter = new LeafSetter(conversions, new MethodWrapper(method), collisions);
				this.addRootSetter(path, setter, true);
			}
		}
		
		/**
		 * Check out a field.  Note that only leaf fields (ie, non-embedded) complete a Setter
		 * chain and thus result in one getting added to the rootSetters.  Until then all Setter
		 * chains are in limbo.
		 * 
		 * @param field must be a proper persistable field.
		 */
		void visitField(Field field, Collection<String> names)
		{
			List<String> paths = this.namesToPaths(names);

			if (TypeUtils.isEmbedded(field))
			{
				if (field.getType().isArray())
				{
					Class<?> visitType = field.getType().getComponentType();

					for (String path: paths)
					{
						List<String> collisions = makeCollisions(paths, path);
						EmbeddedMultivalueSetter setter = new EmbeddedArraySetter(field, path, collisions);
						this.addNullIndexSetter(setter, path, collisions);
						
						Visitor visitor = new Visitor(this.setterChain.extend(setter), path, this.fieldPathsUsed, this.methodPathsUsed);
						visitor.visitClass(visitType);
					}
				}
				else if (Map.class.isAssignableFrom(field.getType()))
				{
					Class<?> visitType = TypeUtils.getMapValueType(field.getGenericType());

					for (String path : paths)
					{
						List<String> collisions = makeCollisions(paths, path);
						EmbeddedMapSetter setter = new EmbeddedMapSetter(field, visitType, conversions, collisions);

						addRootSetter(path, setter, false);
					}
				}
				else if (Collection.class.isAssignableFrom(field.getType()))
				{
					Class<?> visitType = TypeUtils.getComponentType(field.getType(), field.getGenericType());

					for (String path: paths)
					{
						List<String> collisions = makeCollisions(paths, path);
						EmbeddedMultivalueSetter setter = new EmbeddedCollectionSetter(field, path, collisions);
						this.addNullIndexSetter(setter, path, collisions);
						
						Visitor visitor = new Visitor(this.setterChain.extend(setter), path, this.fieldPathsUsed, this.methodPathsUsed);
						visitor.visitClass(visitType);
					}
				}
				else	// basic class
				{
					Class<?> visitType = field.getType();
					
					for (String path: paths)
					{
						List<String> collisions = makeCollisions(paths, path);
						Setter setter = new EmbeddedClassSetter(field, collisions);
						
						Visitor visitor = new Visitor(this.setterChain.extend(setter), path, this.fieldPathsUsed, this.methodPathsUsed);
						visitor.visitClass(visitType);
					}
				}
			}
			else	// not embedded, so we're at a leaf object (including arrays and collections of basic types)
			{
				for (String path: paths)
				{
					List<String> collisions = makeCollisions(paths, path);
					LeafSetter setter = new LeafSetter(conversions, new FieldWrapper(field), collisions);
					this.addRootSetter(path, setter, false);
				}
			}
		}
		
		/**
		 * Translates names to paths based on the current path prefix.
		 */
		private List<String> namesToPaths(Collection<String> names)
		{
			List<String> paths = new ArrayList<String>();
			for (String name: names)
				paths.add(TypeUtils.extendPropertyPath(this.prefix, name));
			
			return paths;
		}
		
		/** 
		 * Make the list of collisions given the paths without the path; return null
		 * if there is only one path (no collisions). Note that the return type
		 * is always a list because at runtime we want fast iteration.
		 */
		private List<String> makeCollisions(Collection<String> paths, String forPath)
		{
			if (paths.size() > 1)
			{
				List<String> collisions = new ArrayList<String>(paths.size()-1);
				for (String path: paths)
					if (!path.equals(forPath))
						collisions.add(path);

				return collisions;
			}
			else
			{
				return null;
			}
		}
		
		/**
		 * Embedded collections need a null index setter to handle the case of an all-null
		 * collection.
		 */
		void addNullIndexSetter(EmbeddedMultivalueSetter setter, String path, Collection<String> collisionPaths)
		{
			EmbeddedNullIndexSetter nes = new EmbeddedNullIndexSetter(setter, path, collisionPaths);
			this.addRootSetter(TypeUtils.getNullIndexPath(path), nes, false);
		}
		
		/**
		 * Takes a final leaf setter, extends the setter chain so far, and 
		 * Adds a final leaf setter to the setters collection.
		 * @param fullPath is the whole "blah.blah.blah" path for this property
		 * @param method is true if this is setting a method, false if setting a field
		 */
		void addRootSetter(String fullPath, Setter setter, boolean method)
		{
			if (method)
			{
				if (this.methodPathsUsed.contains(fullPath))
					throw new IllegalStateException("Attempting to create multiple associations on " + clazz + " for " + fullPath);
				else
					this.methodPathsUsed.add(fullPath);
			}
			else
			{
				if (this.fieldPathsUsed.contains(fullPath) || this.methodPathsUsed.contains(fullPath))
					throw new IllegalStateException("Attempting to create multiple associations on " + clazz + " for " + fullPath);
				else
					this.fieldPathsUsed.add(fullPath);
			}
			
			// Extend and strip off the unnecessary (at runtime) SetterRoot
			Setter chain = this.setterChain.extend(setter).getNext();
			rootSetters.put(fullPath, chain);
		}
	}
	
	/**
	 * Creats a transmog for the specified class, introspecting it and discovering
	 * how to load/save its properties.
	 */
	public Transmog(Conversions conversions, Class<T> clazz)
	{
		this.conversions = conversions;
		this.clazz = clazz;
		
		// This creates the setters in the rootSetters collection and validates the pojo
		new Visitor().visitClass(clazz);
		
		// Construction of the savers is relatively straighforward
		this.rootSaver = new ClassSaver(conversions, clazz);
	}
	
	/**
	 * Loads the property data in an Entity into a POJO.  Does not affect id/parent
	 * (ie key) fields; those are assumed to already have been set.
	 * 
	 * @param fromEntity is a raw datastore entity
	 * @param toPojo is your typed entity
	 */
	public void load(Entity fromEntity, T toPojo)
	{
		LoadContext context = new LoadContext(toPojo, fromEntity);
		
		for (Map.Entry<String, Object> property: fromEntity.getProperties().entrySet())
		{
			String key = property.getKey();
			Object value = property.getValue();
			loadSingleValue(key, value, toPojo, context);
		}
		
		context.done();
	}

	/**
	 * Loads the single value {@code value} into {@code toPojo} using {@code key} as the key within
	 * the entity. Will use any of {@link #rootSetters} or might delegate through a map setter.
	 * 
	 * @param key the key for this value
	 * @param value the value from the datastore
	 * @param toPojo the target pojo to load into
	 */
	public void loadSingleValue(String key, Object value, Object toPojo, LoadContext context)
	{
		Setter setter = this.rootSetters.get(key);
		if (setter != null)
		{
			setter.set(toPojo, value, context);
		}
		else
		{
			String mapPrefix = key;
			int lastDotIndex = mapPrefix.lastIndexOf('.');
			while (setter == null && lastDotIndex != -1)
			{
				mapPrefix = mapPrefix.substring(0, lastDotIndex);
				setter = this.rootSetters.get(mapPrefix);
				if (setter != null)
				{
					int mapKeyEnd = mapPrefix.length() + 1;
					int followingDot = key.indexOf('.', mapKeyEnd);
					if (followingDot == -1)
					{
						context.currentMapEntry = key.substring(mapKeyEnd);
						context.currentMapSuffix = "";
					}
					else
					{
						context.currentMapEntry = key.substring(mapKeyEnd, followingDot);
						context.currentMapSuffix = key.substring(followingDot + 1);
					}
					break;
				}
				lastDotIndex = mapPrefix.lastIndexOf('.');
			}
			if (setter != null)
			{
				setter.set(toPojo, value, context);
			}
		}
	}
	
	/**
	 * Saves the fields of a POJO into the properties of an Entity.  Does not affect id/parent
	 * (ie key) fields; those are assumed to already have been set.
	 * 
	 * @param fromPojo is your typed entity
	 * @param toEntity is a raw datastore entity
	 */
	public void save(T fromPojo, Entity toEntity)
	{
		// The default is to index all fields
		this.rootSaver.save(fromPojo, toEntity, Path.root(), true);
	}
}
