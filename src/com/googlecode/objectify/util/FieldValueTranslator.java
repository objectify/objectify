package com.googlecode.objectify.util;

import java.lang.reflect.Field;

import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.impl.Node;
import com.googlecode.objectify.impl.Path;
import com.googlecode.objectify.impl.translate.CreateContext;
import com.googlecode.objectify.impl.translate.LoadContext;
import com.googlecode.objectify.impl.translate.SaveContext;
import com.googlecode.objectify.impl.translate.Translator;

/**
 * Builds and wraps a translator with a more convenient interface.  For simple value fields only.  
 */
public class FieldValueTranslator<P, D>
{
	private static final LoadContext NULL_LOAD_CONTEXT = new LoadContext(null);
	private static final SaveContext NULL_SAVE_CONTEXT = new SaveContext(null);
	
	/** */
	Translator<P> wrapped;
	
	/** */
	public FieldValueTranslator(ObjectifyFactory fact, Field field) {
		CreateContext ctx = new CreateContext(fact);
		this.wrapped = fact.getTranslators().create(Path.of(field.getName()), field.getAnnotations(), field.getGenericType(), ctx);
	}
	
	/**
	 * Translates a datastore representation to a pojo representation
	 */
	public P load(D value) {
		return wrapped.load(Node.of(value), NULL_LOAD_CONTEXT);
	}
	
	/**
	 * Translates a pojo representation to a datastore representation
	 */
	@SuppressWarnings("unchecked")
	public D save(P value) {
		return (D)wrapped.save(value, Path.root(), false, NULL_SAVE_CONTEXT).getPropertyValue();
	}
}
