package com.googlecode.objectify.impl.translate;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.appengine.api.datastore.EmbeddedEntity;
import com.google.appengine.api.datastore.PropertyContainer;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.annotation.Owner;
import com.googlecode.objectify.impl.Path;
import com.googlecode.objectify.impl.Property;
import com.googlecode.objectify.impl.TranslatableProperty;
import com.googlecode.objectify.impl.TypeUtils;
import com.googlecode.objectify.util.LogUtils;

/**
 * Translator which knows how to convert a POJO into a PropertiesContainer (ie, Entity or EmbeddedEntity).
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class ClassTranslator<T> implements Translator<T>
{
	private static final Logger log = Logger.getLogger(ClassTranslator.class.getName());

	/** */
	protected final ObjectifyFactory fact;
	protected final Class<T> clazz;
	protected final List<TranslatableProperty<Object>> props = new ArrayList<TranslatableProperty<Object>>();
	
	/** Any owner properties, if they exist */
	protected final List<Property> owners = new ArrayList<Property>();

	/** */
	public ClassTranslator(Class<T> clazz, Path path, CreateContext ctx)
	{
		this.fact = ctx.getFactory();
		this.clazz = clazz;

		if (log.isLoggable(Level.FINEST))
			log.finest("Creating class translator for " + clazz.getName() + " at path '"+ path + "'");

		// Quick sanity check - can we construct one of these?  If not, blow up.  But allow abstract base classes!
		if (!Modifier.isAbstract(clazz.getModifiers())) {
			try {
				fact.construct(clazz);
			} catch (Exception ex) {
				throw new IllegalStateException("Unable to construct an instance of " + clazz.getName() + "; perhaps it has no suitable constructor?", ex);
			}
		}

//		ctx.enterOwnerContext(clazz);
//		try {
			for (Property prop: TypeUtils.getProperties(fact, clazz)) {
				Path propPath = path.extend(prop.getName());
				try {
					if (prop.getAnnotation(Owner.class) != null) {
//						ctx.verifyOwnerProperty(propPath, prop);
						owners.add(prop);
					} else {
						Translator<Object> loader = fact.getTranslators().create(propPath, prop, prop.getType(), ctx);
						TranslatableProperty<Object> tprop = new TranslatableProperty<Object>(prop, loader);
						props.add(tprop);
		
						this.foundTranslatableProperty(tprop);
					}
				} catch (Exception ex) {
					// Catch any errors during this process and wrap them in an exception that exposes more useful information.
					propPath.throwIllegalState("Error registering " + clazz.getName(), ex);
				}
//			}
//		} finally {
//			ctx.exitOwnerContext(clazz);
//		}
	}

	/** */
	public Class<?> getTranslatedClass() { return this.clazz; }

	/**
	 * Called when each property is discovered, allows a subclass to do something special with it
	 */
	protected void foundTranslatableProperty(TranslatableProperty<Object> tprop) {}

	@Override
	public T load(Object node, LoadContext ctx) throws SkipException {
		if (log.isLoggable(Level.FINEST))
			log.finest(LogUtils.msg(node.getPath(), "Instantiating a " + clazz.getName()));

		T pojo = fact.construct(clazz);
		
		// Load any optional owner properties (only applies when this is an embedded class)
		for (Property ownerProp: owners) {
			Object owner = ctx.getOwner(ownerProp);

			if (log.isLoggable(Level.FINEST))
				log.finest(LogUtils.msg(node.getPath(), "Setting property " + ownerProp.getName() + " to " + owner));
			
			ownerProp.set(pojo, owner);
		}

		// On with the normal show
		ctx.enterOwnerContext(pojo);;
		try {
			for (TranslatableProperty<Object> prop: props) {
				prop.executeLoad(node, pojo, ctx);
			}
		} finally {
			ctx.exitOwnerContext(pojo);
		}

		return pojo;
	}

	@Override
	public Object save(T pojo, Path path, boolean index, SaveContext ctx) throws SkipException {
		PropertyContainer container = constructContainer();

		for (TranslatableProperty<Object> prop: props) {
			prop.executeSave(pojo, container, path, index, ctx);
		}

		return container;
	}

	/**
	 *
	 */
	protected PropertyContainer constructContainer() {
		return new EmbeddedEntity();
	}
}
}