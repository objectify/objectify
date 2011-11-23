package com.googlecode.objectify.impl.load;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import javax.jdo.metadata.FieldMetadata;

import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.impl.LoadContext;
import com.googlecode.objectify.impl.Loadable;
import com.googlecode.objectify.impl.TypeUtils;


/**
 * <p>Loader which discovers how to load a class.  Can handle both the root class (because the key fields get stuffed
 * into the EntityNode) and simple (non-collection) embedded classes (which don't recognze @Id/@Parent as significant).</p>
 */
public class ClassLoadr<T> implements Loader
{
	/** */
	ObjectifyFactory fact;
	
	/** */
	Class<T> entityClass;
	
	/** Classes are composed of fields, duh */
	List<FieldLoader> fieldLoaders = new ArrayList<FieldLoader>();
	
	/**
	 * @param clazz is the class we want to load.
	 */
	public ClassLoadr(ObjectifyFactory fact, Class<T> clazz)
	{
		List<Loadable> loadables = TypeUtils.getLoadables(clazz);

		for (Loadable loadable: loadables) {
			Loader loader = new LoadableLoader(fact, loadable);
			
			if (loadable.isEmbed()) {
				
//				if (field.getType().isArray())
//				{
//					Loader loader = new EmbeddedArrayFieldLoader(conv, clazz, field);
//					this.fieldLoaders.add(saver);
//				}
//				else if (Map.class.isAssignableFrom(field.getType()))
//				{
//					Loader loader = new EmbeddedMapLoader(conv, clazz, field);
//					this.fieldLoaders.add(saver);
//				}
//				else if (Collection.class.isAssignableFrom(field.getType()))
//				{
//					Loader loader = new EmbeddedCollectionFieldLoader(conv, clazz, field);
//					this.fieldLoaders.add(saver);
//				}
//				else	// basic class
//				{
//					Loader loader = new EmbeddedClassFieldLoader(conv, clazz, field);
//					this.fieldLoaders.add(saver);
//				}
			}
			else	// not embedded, so we're at a leaf object (including arrays and collections of basic types)
			{
				// Add a leaf saver
				Loader saver = new LeafFieldLoader(conv, clazz, field);
				this.fieldLoaders.add(saver);
			}
		}
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.load.Loader#load(com.googlecode.objectify.impl.load.EntityNode, com.googlecode.objectify.impl.LoadContext)
	 */
	@Override
	public T load(EntityNode node, LoadContext ctx)
	{
		T pojo = fact.construct(entityClass);
		
		for (FieldLoader fieldLoader: this.fieldLoaders) {
			
			Object val = node.get(fieldLoader.getFieldName());
			fieldLoader.load(val, pojo, path);
		}
	}
	
	/**
	 * @param parent is the collection in which to look
	 * @param names is a list of names to look for in the parent 
	 * @return one child which has a name in the parent
	 * @throws IllegalStateException if there are multiple name matches
	 */
	private EntityNode getChild(EntityNode parent, String[] names) {
		EntityNode child = null;
		
		for (String name: names) {
			EntityNode child2 = parent.get(name);
			
			if (child != null && child2 != null)
				throw new IllegalStateException("Collision trying to load field; multiple name matches at " + parent.getPath());
			
			child = child2;
		}
		
		return child;
	}
}
