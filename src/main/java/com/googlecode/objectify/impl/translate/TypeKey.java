package com.googlecode.objectify.impl.translate;

import com.google.common.base.Objects;
import com.googlecode.objectify.impl.Property;
import com.googlecode.objectify.repackaged.gentyref.GenericTypeReflector;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Arrays;

/**
 * <p>Unique identifier for a translator instance. Important so we can re-use translators
 * as we navigate the tree, allowing us to store recursive structures.</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class TypeKey<T>
{
	/** */
	private static final Annotation[] NO_ANNOTATIONS = {};

	/** */
	private Type type;
	public Type getType() { return type; }

	/** */
	private Annotation[] annotations;
	public Annotation[] getAnnotations() { return annotations; }

	/** */
	public TypeKey(Type type) {
		this(type, NO_ANNOTATIONS);
	}

	/** */
	public TypeKey(Type type, Annotation[] annotations) {
		this.type = type;
		this.annotations = annotations;
	}

	/**
	 * Create a new typekey for a new type but which preserves characteristics of the old typekey (ie annotations).
	 * This is used when creating a new typekey for component types.
	 */
	public TypeKey(Type type, TypeKey previous) {
		this(type, previous.getAnnotations());
	}

	/**
	 * Create a typekey from a property
	 */
	public TypeKey(Property prop) {
		this(prop.getType(), prop.getAnnotations());
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
			
		TypeKey other = (TypeKey)obj;

		return type.equals(other.type) && Arrays.equals(annotations, other.annotations);
	}

	@Override
	public int hashCode() {
		int code = type.hashCode();

		for (Annotation annotation: annotations)
			code = 31 * code + annotation.hashCode();

		return code;
	}

	/**
	 * Get the basic class through erasure.
	 */
	@SuppressWarnings("unchecked")
	public Class<T> getTypeAsClass() {
		return (Class<T>)GenericTypeReflector.erase(type);
	}

	/** Gets the annotation that has the specified type, or null if there isn't one */
	@SuppressWarnings("unchecked")
	public <A extends Annotation> A getAnnotation(Class<A> annotationType) {
		for (Annotation anno: annotations)
			if (annotationType.isAssignableFrom(anno.getClass()))
				return (A)anno;

		return null;
	}

	/** @return true if the annotation is present */
	public boolean isAnnotationPresent(Class<? extends Annotation> annotationType) {
		return getAnnotation(annotationType) != null;
	}

	/**
	 * Checks not only the listed annotations but also annotations on the class.
	 */
	public <A extends Annotation> A getAnnotationAnywhere(Class<A> annotationType) {
		A anno = getAnnotation(annotationType);
		if (anno == null) {
			Class<?> clazz = (Class<?>) GenericTypeReflector.erase(type);
			return clazz.getAnnotation(annotationType);
		} else {
			return anno;
		}
	}

	/**
	 * Can this type be assigned to a variable with the specified type?
	 */
	public boolean isAssignableTo(Class<?> superclass) {
		return superclass.isAssignableFrom(getTypeAsClass());
	}

	/* */
	@Override
	public String toString() {
		return Objects.toStringHelper(this)
				.add("type", type)
				.add("annotations", Arrays.toString(annotations))
				.toString();
	}
}
