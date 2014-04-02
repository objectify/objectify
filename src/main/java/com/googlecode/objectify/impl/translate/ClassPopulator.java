package com.googlecode.objectify.impl.translate;

import com.google.appengine.api.datastore.PropertyContainer;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.Owner;
import com.googlecode.objectify.annotation.Unindex;
import com.googlecode.objectify.impl.Path;
import com.googlecode.objectify.impl.Property;
import com.googlecode.objectify.impl.PropertyPopulator;
import com.googlecode.objectify.impl.TypeUtils;
import com.googlecode.objectify.util.LogUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <p>Used by translators to populate properties between POJO and PropertiesContainer. Unlike
 * translators, this does not create the POJO or container, it just copies translated properties
 * between them.</p>
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class ClassPopulator<P> implements Populator<P>
{
	private static final Logger log = Logger.getLogger(ClassPopulator.class.getName());

	/** Populator for the superclass */
	private final Populator<? super P> superPopulator;

	/** Only includes fields declared on this class */
	private final List<PropertyPopulator<Object, Object>> props = new ArrayList<>();

	/** Any owner properties, if they exist */
	private final List<Property> owners = new ArrayList<>();

	/** Three-state index instruction for the whole class. Null means "leave it as-is". */
	private final Boolean indexInstruction;

	/** */
	public ClassPopulator(Class<P> clazz, CreateContext ctx, Path path) {
		// First thing we do is recursively climb the superclass chain
		this.superPopulator = ctx.getPopulator(clazz.getSuperclass(), path);

		if (log.isLoggable(Level.FINEST))
			log.finest("Creating class translator for " + clazz.getName() + " at path '"+ path + "'");

		indexInstruction = getIndexInstruction(clazz);

		for (Property prop: TypeUtils.getDeclaredProperties(ctx.getFactory(), clazz)) {
			Path propPath = path.extend(prop.getName());
			try {
				if (prop.getAnnotation(Owner.class) != null) {
					owners.add(prop);
				} else {
					Translator<Object, Object> translator = ctx.getTranslator(prop.getType(), prop.getAnnotations(), ctx, propPath);
					PropertyPopulator<Object, Object> tprop = new PropertyPopulator<>(prop, translator);

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
	protected boolean consider(PropertyPopulator<Object, Object> tprop) { return true; }

	/* */
	@Override
	public void load(PropertyContainer node, LoadContext ctx, Path path, P into) {
		superPopulator.load(node, ctx, path, into);

		// Load any optional owner properties (only applies when this is an embedded class)
		for (Property ownerProp: owners) {
			Object owner = ctx.getOwner(ownerProp);

			if (log.isLoggable(Level.FINEST))
				log.finest(LogUtils.msg(path, "Setting owner property " + ownerProp.getName() + " to " + owner));

			ownerProp.set(into, owner);
		}

		// On with the normal show
		ctx.enterOwnerContext(into);
		try {
			for (PropertyPopulator<Object, Object> prop: props) {
				prop.load(node, ctx, path, into);
			}
		} finally {
			ctx.exitOwnerContext(into);
		}
	}

	/* */
	@Override
	public void save(P pojo, boolean index, SaveContext ctx, Path path, PropertyContainer into) {
		superPopulator.save(pojo, index, ctx, path, into);

		if (indexInstruction != null)
			index = indexInstruction;

		for (PropertyPopulator<Object, Object> prop: props) {
			prop.save(pojo, index, ctx, path, into);
		}
	}
}
