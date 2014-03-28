package com.googlecode.objectify.impl.translate;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.appengine.api.datastore.EmbeddedEntity;
import com.google.appengine.api.datastore.PropertyContainer;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.Owner;
import com.googlecode.objectify.annotation.Parent;
import com.googlecode.objectify.annotation.Unindex;
import com.googlecode.objectify.impl.KeyMetadata;
import com.googlecode.objectify.impl.Path;
import com.googlecode.objectify.impl.Property;
import com.googlecode.objectify.impl.TranslatableProperty;
import com.googlecode.objectify.impl.TypeUtils;
import com.googlecode.objectify.util.LogUtils;

/**
 * Translator which knows how to convert a POJO into a PropertiesContainer (ie, Entity or EmbeddedEntity).
 * Which one (Entity or EmbeddedEntity) will depend on whether or not we are at the root path.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class ClassTranslator<P> implements Translator<P, PropertyContainer>
{
	private static final Logger log = Logger.getLogger(ClassTranslator.class.getName());

	/** */
	private final ObjectifyFactory fact;
	protected final Class<P> clazz;
	private final List<TranslatableProperty<Object, Object>> props = new ArrayList<>();

	/** Three-state index instruction for the whole class. Null means "leave it as-is". */
	private final Boolean indexInstruction;
	
	/** Any owner properties, if they exist */
	private final List<Property> owners = new ArrayList<>();

	/** */
	public ClassTranslator(Class<P> clazz, Path path, CreateContext ctx) {
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

		indexInstruction = getIndexInstruction(clazz);

		// Look for the key metadata
		TranslatableProperty<Object, Object> idMeta = null;
		TranslatableProperty<Object, Object> parentMeta = null;

		for (Property prop: TypeUtils.getProperties(fact, clazz)) {
			Path propPath = path.extend(prop.getName());
			try {
				if (prop.getAnnotation(Owner.class) != null) {
					owners.add(prop);
				} else {
					Translator<Object, Object> translator = fact.getTranslators().get(prop, prop.getType(), ctx, propPath);
					TranslatableProperty<Object, Object> tprop = new TranslatableProperty<>(prop, translator);

					if (consider(tprop))
						props.add(tprop);
				}
			} catch (Exception ex) {
				// Catch any errors during this process and wrap them in an exception that exposes more useful information.
				propPath.throwIllegalState("Error registering " + clazz.getName(), ex);
			}
		}
	}

	/**
	 * Figure out if there is an index instruction for the whole class.
	 * @return true, false, or null (which means no info)
	 */
	private Boolean getIndexInstruction(Class<P> clazz) {
		Index ind = clazz.getAnnotation(Index.class);
		Unindex unind = clazz.getAnnotation(Unindex.class);

		if (ind != null && unind != null)
			throw new IllegalStateException("You cannot have @Index and @Unindex on the same class: " + clazz);

		if (ind != null)
			return true;
		else if (unind != null)
			return false;
		else
			return null;
	}

	/**
	 * Called when each property is discovered, allows a subclass to do something special with it
	 * @return false if the property should not be considered a standard populated property.
	 */
	protected boolean consider(TranslatableProperty<Object, Object> tprop) { return true; }

	@Override
	public P load(PropertyContainer node, LoadContext ctx) throws SkipException {
		if (log.isLoggable(Level.FINEST))
			log.finest("Instantiating a " + clazz.getName()));

		P pojo = fact.construct(clazz);
		
		// Load any optional owner properties (only applies when this is an embedded class)
		for (Property ownerProp: owners) {
			Object owner = ctx.getOwner(ownerProp);

			if (log.isLoggable(Level.FINEST))
				log.finest(LogUtils.msg(node.getPath(), "Setting property " + ownerProp.getName() + " to " + owner));
			
			ownerProp.set(pojo, owner);
		}

		// On with the normal show
		ctx.enterOwnerContext(pojo);
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
	public PropertyContainer save(P pojo, boolean index, SaveContext ctx, Path path) throws SkipException {
		if (indexInstruction != null)
			index = indexInstruction;

		PropertyContainer container = constructContainer(path);

		for (TranslatableProperty<Object, Object> prop: props) {
			prop.executeSave(pojo, container, path, index, ctx);
		}

		return container;
	}

	/**
	 *
	 */
	protected PropertyContainer constructContainer(Path path) {
		if (path.isRoot())
			return new Entity();
		else
			return new EmbeddedEntity();
	}
}
