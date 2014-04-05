package com.googlecode.objectify.impl.translate;

import com.google.appengine.api.datastore.PropertyContainer;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.annotation.AlsoLoad;
import com.googlecode.objectify.annotation.Ignore;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.OnLoad;
import com.googlecode.objectify.annotation.OnSave;
import com.googlecode.objectify.annotation.Unindex;
import com.googlecode.objectify.impl.FieldProperty;
import com.googlecode.objectify.impl.MethodProperty;
import com.googlecode.objectify.impl.Path;
import com.googlecode.objectify.impl.Property;
import com.googlecode.objectify.impl.PropertyPopulator;
import com.googlecode.objectify.impl.TypeUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
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

	/** We do not persist fields with any of these modifiers */
	private static final int NOT_SAVEABLE_MODIFIERS = Modifier.FINAL | Modifier.STATIC;

	/** */
	private final Class<P> clazz;

	/** Populator for the superclass */
	private final Populator<? super P> superPopulator;

	/** Only includes fields declared on this class */
	private final List<PropertyPopulator<Object, Object>> props = new ArrayList<>();

	/** Three-state index instruction for the whole class. Null means "leave it as-is". */
	private final Boolean indexInstruction;

	/** */
	private final List<LifecycleMethod> onSaveMethods = new ArrayList<>();
	private final List<LifecycleMethod> onLoadMethods = new ArrayList<>();

	/**
	 * Includes every property
	 */
	public ClassPopulator(Class<P> clazz, CreateContext ctx, Path path) {
		this(clazz, ctx, path, Predicates.<Property>alwaysTrue());
	}

	/** */
	public ClassPopulator(Class<P> clazz, CreateContext ctx, Path path, Predicate<Property> include) {
		this.clazz = clazz;

		// Recursively climb the superclass chain
		this.superPopulator = ctx.getPopulator(clazz.getSuperclass(), path);

		if (log.isLoggable(Level.FINEST))
			log.finest("Creating class translator for " + clazz.getName() + " at path '"+ path + "'");

		indexInstruction = getIndexInstruction(clazz);

		// Find all the basic properties
		for (Property prop: getDeclaredProperties(ctx.getFactory(), clazz)) {
			Path propPath = path.extend(prop.getName());
			try {
				Translator<Object, Object> translator = ctx.getTranslator(new TypeKey<>(prop), ctx, propPath);
				PropertyPopulator<Object, Object> tprop = new PropertyPopulator<>(prop, translator);

				if (include.apply(prop))
					props.add(tprop);

			} catch (Exception ex) {
				// Catch any errors during this process and wrap them in an exception that exposes more useful information.
				propPath.throwIllegalState("Error registering " + clazz.getName(), ex);
			}
		}

		// Find the @OnSave methods
		for (Method method: clazz.getDeclaredMethods()) {
			if (method.isAnnotationPresent(OnSave.class))
				onSaveMethods.add(new LifecycleMethod(method));

			if (method.isAnnotationPresent(OnLoad.class))
				onLoadMethods.add(new LifecycleMethod(method));
		}
	}

	/* */
	@Override
	public void load(PropertyContainer node, LoadContext ctx, Path path, final P into) {
		superPopulator.load(node, ctx, path, into);

		ctx.enterOwnerContext(into);
		try {
			for (PropertyPopulator<Object, Object> prop: props) {
				prop.load(node, ctx, path, into);
			}
		} finally {
			ctx.exitOwnerContext(into);
		}

		// If there are any @OnLoad methods, call them after everything else
		if (!onLoadMethods.isEmpty()) {
			ctx.defer(new Runnable() {
				@Override
				public void run() {
					for (LifecycleMethod method: onLoadMethods)
						method.execute(into);
				}

				@Override
				public String toString() {
					return "(deferred invoke " + clazz + " @OnLoad callbacks on " + into + ")";
				}
			});
		}
	}

	/* */
	@Override
	public void save(P pojo, boolean index, SaveContext ctx, Path path, PropertyContainer into) {
		// Must do @OnSave methods first
		if (!onSaveMethods.isEmpty())
			for (LifecycleMethod method: onSaveMethods)
				method.execute(pojo);

		superPopulator.save(pojo, index, ctx, path, into);

		if (indexInstruction != null)
			index = indexInstruction;

		for (PropertyPopulator<Object, Object> prop: props) {
			prop.save(pojo, index, ctx, path, into);
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
	 * Determine if we should create a Property for the field.  Things we ignore:  static, final, @Ignore, synthetic
	 */
	private boolean isOfInterest(Field field) {
		return !field.isAnnotationPresent(Ignore.class)
				&& ((field.getModifiers() & NOT_SAVEABLE_MODIFIERS) == 0)
				&& !field.isSynthetic()
				&& !field.getName().startsWith("bitmap$init");	// Scala adds a field bitmap$init$0 and bitmap$init$1 etc
	}

	/**
	 * Determine if we should create a Property for the method (ie, @AlsoLoad)
	 */
	private  boolean isOfInterest(Method method) {
		for (Annotation[] annos: method.getParameterAnnotations())
			if (TypeUtils.getAnnotation(AlsoLoad.class, annos) != null)
				return true;

		return false;
	}

	/**
	 * Get all the persistable fields and methods declared on a class. Ignores superclasses.
	 *
	 * @return the fields we load and save, including @Id and @Parent fields. All fields will be set accessable
	 *  and returned in order of declaration.
	 */
	private List<Property> getDeclaredProperties(ObjectifyFactory fact, Class<?> clazz) {
		List<Property> good = new ArrayList<>();

		for (Field field: clazz.getDeclaredFields())
			if (isOfInterest(field))
				good.add(new FieldProperty(fact, clazz, field));

		for (Method method: clazz.getDeclaredMethods())
			if (isOfInterest(method))
				good.add(new MethodProperty(method));

		return good;
	}
}
