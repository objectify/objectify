package com.googlecode.objectify.impl.translate;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Arrays;

/**
 * <p>Unique identifier for a translator instance. Important so we can re-use translators
 * as we navigate the tree, allowing us to store recursive structures.</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class TranslatorKey
{
	/** */
	private static final Annotation[] NO_ANNOTATIONS = {};

	/** */
	private Type type;

	/** */
	private Annotation[] annotations;

	/** */
	public TranslatorKey(Type type) {
		this(type, NO_ANNOTATIONS);
	}

	/** */
	public TranslatorKey(Type type, Annotation[] annotations) {
		this.type = type;
		this.annotations = annotations;
	}

	@Override
	public boolean equals(Object obj) {
		TranslatorKey other = (TranslatorKey)obj;

		return type.equals(other.type) && Arrays.equals(annotations, other.annotations);
	}

	@Override
	public int hashCode() {
		int code = type.hashCode();

		for (Annotation annotation: annotations)
			code = 31 * code + annotation.hashCode();

		return code;
	}
}
