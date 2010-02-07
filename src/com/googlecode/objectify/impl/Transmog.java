package com.googlecode.objectify.impl;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.appengine.api.datastore.Entity;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.annotation.OldName;
import com.googlecode.objectify.impl.load.EmbeddedArraySetter;
import com.googlecode.objectify.impl.load.EmbeddedClassSetter;
import com.googlecode.objectify.impl.load.EmbeddedCollectionSetter;
import com.googlecode.objectify.impl.load.EmbeddedMultivalueSetter;
import com.googlecode.objectify.impl.load.EmbeddedNullIndexSetter;
import com.googlecode.objectify.impl.load.LeafSetter;
import com.googlecode.objectify.impl.load.RootSetter;
import com.googlecode.objectify.impl.load.Setter;
import com.googlecode.objectify.impl.save.ClassSaver;

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
	/** Needed to convert Key types */
	ObjectifyFactory factory;
	
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
		
		/** Constructs a visitor for a top-level entity */
		public Visitor()
		{
			this.setterChain = new RootSetter();
		}
		
		/**
		 * Constructs a visitor for an embedded object.
		 * @param setterChain is the root of the setter chain
		 */
		public Visitor(Setter setterChain, String prefix)
		{
			this.setterChain = setterChain;
			this.prefix = prefix;
			this.embedded = true;
		}
		
		/**
		 * Creates a set of Setters for the class (and parent/embedded classes) in the
		 * Transmog.rootSetters collection
		 * 
		 * @param clazz is the class to inspect
		 */
		public void visitClass(Class<?> clazz)
		{
			// Only good fields come back from this method call
			List<Field> fields = TypeUtils.getPesistentFields(clazz);
			for (Field field: fields)
				this.visitField(field);

			// Only good methods come back from this method call
			Map<String, Method> methods = TypeUtils.getOldNameMethods(clazz);
			for (Map.Entry<String, Method> method: methods.entrySet())
				this.visitMethod(method.getKey(), method.getValue());
		}
		
		/**
		 * @param name is the oldName value
		 * @param method must be a proper @OldName field.
		 */
		void visitMethod(String name, Method method)
		{
			String path = TypeUtils.extendPropertyPath(this.prefix, name);
			LeafSetter setter = new LeafSetter(factory, new MethodWrapper(method), null);
			
			this.addRootSetter(path, setter);
		}
		
		/**
		 * Check out a field.  Note that only leaf fields (ie, non-embedded) complete a Setter
		 * chain and thus result in one getting added to the rootSetters.  Until then all Setter
		 * chains are in limbo.
		 * 
		 * @param field must be a proper persistable field.
		 */
		void visitField(Field field)
		{
			String path = TypeUtils.extendPropertyPath(this.prefix, field.getName());
			
			if (TypeUtils.isEmbedded(field))
			{
				// Might have one of these,
				OldName oldName = field.getAnnotation(OldName.class);
				
				if (field.getType().isArray())
				{
					Class<?> visitType = field.getType().getComponentType();
					
					EmbeddedMultivalueSetter setter = new EmbeddedArraySetter(field, path, null);
					this.addNullIndexSetter(setter, path, null);
					
					Visitor visitor = new Visitor(this.setterChain.extend(setter), path);
					visitor.visitClass(visitType);
					
					if (oldName != null)
					{
						EmbeddedMultivalueSetter oldNameSetter = new EmbeddedArraySetter(field, oldName.value(), path);
						this.addNullIndexSetter(oldNameSetter, oldName.value(), path);
						
						Visitor oldNameVisitor = new Visitor(this.setterChain.extend(setter), oldName.value());
						oldNameVisitor.visitClass(visitType);
					}
				}
				else if (Collection.class.isAssignableFrom(field.getType()))
				{
					Class<?> visitType = TypeUtils.getComponentType(field.getType(), field.getGenericType());
					
					EmbeddedMultivalueSetter setter = new EmbeddedCollectionSetter(field, path, null);
					this.addNullIndexSetter(setter, path, null);
					
					Visitor visitor = new Visitor(this.setterChain.extend(setter), path);
					visitor.visitClass(visitType);
					
					if (oldName != null)
					{
						EmbeddedMultivalueSetter oldNameSetter = new EmbeddedCollectionSetter(field, oldName.value(), path);
						this.addNullIndexSetter(oldNameSetter, oldName.value(), path);
						
						Visitor oldNameVisitor = new Visitor(this.setterChain.extend(setter), oldName.value());
						oldNameVisitor.visitClass(visitType);
					}
				}
				else	// basic class
				{
					Class<?> visitType = field.getType();
					Setter setter = new EmbeddedClassSetter(field, null);
					
					Visitor visitor = new Visitor(this.setterChain.extend(setter), path);
					visitor.visitClass(visitType);
					
					if (oldName != null)
					{
						Visitor oldNameVisitor = new Visitor(this.setterChain.extend(setter), oldName.value());
						oldNameVisitor.visitClass(visitType);
					}
				}
			}
			else	// not embedded, so we're at a leaf object (including arrays and collections of basic types)
			{
				// Add a root setter based on the leaf setter
				LeafSetter setter = new LeafSetter(factory, new FieldWrapper(field), null);
				
				this.addRootSetter(path, setter);
				
				OldName oldName = field.getAnnotation(OldName.class);
				if (oldName != null)
				{
					LeafSetter oldNameSetter = new LeafSetter(factory, new FieldWrapper(field), path);
					this.addRootSetter(TypeUtils.extendPropertyPath(this.prefix, oldName.value()), oldNameSetter);	// alternate path
				}
			}
		}
		
		/**
		 * Embedded collections need a null index setter to handle the case of an all-null
		 * collection.
		 */
		void addNullIndexSetter(EmbeddedMultivalueSetter setter, String path, String collisionPath)
		{
			EmbeddedNullIndexSetter nes = new EmbeddedNullIndexSetter(setter, path, collisionPath);
			this.addRootSetter(TypeUtils.getNullIndexPath(path), nes);
		}
		
		/**
		 * Takes a final leaf setter, extends the setter chain so far, and 
		 * Adds a final leaf setter to the setters collection.
		 * @param fullPath is the whole "blah.blah.blah" path for this property
		 */
		void addRootSetter(String fullPath, Setter setter)
		{
			if (rootSetters.containsKey(fullPath))
				throw new IllegalStateException("Attempting to create multiple associations for " + fullPath);

			// Extend and strip off the unnecessary (at runtime) SetterRoot
			Setter chain = this.setterChain.extend(setter).getNext();
			rootSetters.put(fullPath, chain);
		}
	}
	
	/**
	 * Creats a transmog for the specified class, introspecting it and discovering
	 * how to load/save its properties.
	 */
	public Transmog(ObjectifyFactory fact, Class<T> clazz)
	{
		this.factory = fact;
		
		// This creates the setters in the rootSetters collection and validates the pojo
		new Visitor().visitClass(clazz);
		
		// Construction of the savers is relatively straighforward
		this.rootSaver = new ClassSaver(fact, clazz);
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
			Setter setter = this.rootSetters.get(property.getKey());
			if (setter != null)
				setter.set(toPojo, property.getValue(), context);
		}
		
		context.done();
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
		this.rootSaver.save(fromPojo, toEntity);
	}
}
