package com.googlecode.objectify.impl.translate;

import com.google.cloud.datastore.FullEntity;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.annotation.AlsoLoad;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Ignore;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.Namespace;
import com.googlecode.objectify.annotation.OnLoad;
import com.googlecode.objectify.annotation.OnSave;
import com.googlecode.objectify.annotation.Parent;
import com.googlecode.objectify.annotation.Unindex;
import com.googlecode.objectify.impl.FieldProperty;
import com.googlecode.objectify.impl.KeyMetadata;
import com.googlecode.objectify.impl.KeyPopulator;
import com.googlecode.objectify.impl.MethodProperty;
import com.googlecode.objectify.impl.Path;
import com.googlecode.objectify.impl.Property;
import com.googlecode.objectify.impl.PropertyPopulator;
import com.googlecode.objectify.impl.TypeUtils;
import lombok.extern.slf4j.Slf4j;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>Used by translators to populate properties between POJO and PropertiesContainer. Unlike
 * translators, this does not create the POJO or container, it just copies translated properties
 * between them.</p>
 *
 * <p>Always excludes the key fields, @Id and @Parent.</p>
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
@Slf4j
public class ClassPopulator<P> implements Populator<P>
{
	/** We do not persist fields with any of these modifiers */
	private static final int NOT_SAVEABLE_MODIFIERS = Modifier.FINAL | Modifier.STATIC;

	/** We don't want to include the key fields in population */
	private static final Predicate<Property> INCLUDED_FIELDS = prop -> prop.getAnnotation(Id.class) == null && prop.getAnnotation(Parent.class) == null && prop.getAnnotation(Namespace.class) == null;

	/** */
	private final Class<P> clazz;

	/** Populator for the superclass */
	private final Populator<? super P> superPopulator;

	/** Only includes fields declared on this class */
	private final List<Populator<Object>> props = new ArrayList<>();

	/** Three-state index instruction for the whole class. Null means "leave it as-is". */
	private final Boolean indexInstruction;

	/** */
	private final List<LifecycleMethod> onSaveMethods = new ArrayList<>();
	private final List<LifecycleMethod> onLoadMethods = new ArrayList<>();

	/**
	 */
	public ClassPopulator(final Class<P> clazz, final CreateContext ctx, final Path path) {
		this.clazz = clazz;

		// Recursively climb the superclass chain
		this.superPopulator = ctx.getPopulator(clazz.getSuperclass(), path);

		log.trace("Creating class translator for {} at path '{}'", clazz.getName(), path);

		if (clazz.isAnnotationPresent(Entity.class)) {
			@SuppressWarnings("unchecked")
			final Populator<Object> keyPopulator = (Populator<Object>)new KeyPopulator<>(clazz, ctx, path);
			props.add(keyPopulator);
		}

		indexInstruction = getIndexInstruction(clazz);

		// Find all the basic properties
		for (final Property prop: getDeclaredProperties(ctx.getFactory(), clazz)) {
			if (INCLUDED_FIELDS.apply(prop)) {
				final Path propPath = path.extend(prop.getName());
				try {
					final Translator<Object, Object> translator = ctx.getTranslator(new TypeKey<>(prop), ctx, propPath);
					final PropertyPopulator<Object, Object> tprop = new PropertyPopulator<>(prop, translator);
					props.add(tprop);
				} catch (Exception ex) {
					// Catch any errors during this process and wrap them in an exception that exposes more useful information.
					propPath.throwIllegalState("Error registering " + clazz.getName(), ex);
				}
			}
		}

		// Find the @OnSave methods
		for (final Method method: clazz.getDeclaredMethods()) {
			if (method.isAnnotationPresent(OnSave.class))
				onSaveMethods.add(new LifecycleMethod(method));

			if (method.isAnnotationPresent(OnLoad.class))
				onLoadMethods.add(new LifecycleMethod(method));
		}
	}

	/* */
	@Override
	public void load(FullEntity<?> node, LoadContext ctx, Path path, final P into) {
		superPopulator.load(node, ctx, path, into);

		ctx.enterContainerContext(into);
		try {
			for (final Populator<Object> prop: props) {
				prop.load(node, ctx, path, into);
			}
		} finally {
			ctx.exitContainerContext(into);
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
	public void save(P pojo, boolean index, SaveContext ctx, Path path, FullEntity.Builder<?> into) {

		superPopulator.save(pojo, index, ctx, path, into);

		// Must do @OnSave methods after superclass but before actual population
		if (!ctx.skipLifecycle() && !onSaveMethods.isEmpty())
			for (LifecycleMethod method: onSaveMethods)
				method.execute(pojo);

		if (indexInstruction != null)
			index = indexInstruction;

		for (final Populator<Object> prop: props) {
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
			if (TypeUtils.getAnnotation(annos, AlsoLoad.class) != null)
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

	/**
	 * Gets the key metadata but only if this was an @Entity annotated class. Should not be called if not.
	 */
	public KeyMetadata<P> getKeyMetadata() {
		final Populator<Object> populator = props.get(0);
		Preconditions.checkState(populator instanceof KeyPopulator, "Cannot get KeyMetadata for non-@Entity class " + this.clazz);
		return ((KeyPopulator<P>)populator).getKeyMetadata();
	}
}
