package com.googlecode.objectify.impl.translate;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import com.googlecode.objectify.annotation.Translate;
import com.googlecode.objectify.impl.Path;
import com.googlecode.objectify.impl.Property;
import com.googlecode.objectify.impl.TypeUtils;


/**
 * <p>Translator factory which lets users define a custom translator for a field.</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class TranslateTranslatorFactory implements TranslatorFactory<Object, Object>
{
	boolean earlyOnly;
	
	/**
	 * @param earlyOnly determines whether this instance ignores @Translate annotations with early=false
	 */
	public TranslateTranslatorFactory(boolean earlyOnly) {
		this.earlyOnly = earlyOnly;
	}
	
	@Override
	public Translator<Object, Object> create(Type type, Annotation[] annotations, CreateContext ctx, Path path) {
		final Translate translateAnno = TypeUtils.getAnnotation(Translate.class, annotations);

		if (translateAnno == null)
			return null;

		if (earlyOnly && !translateAnno.early())
			return null;

		@SuppressWarnings("unchecked")
		TranslatorFactory<Object, Object> transFact = (TranslatorFactory<Object, Object>)ctx.getFactory().construct(translateAnno.value());

		Translator<Object, Object> trans = transFact.create(type, annotations, ctx, path);
		if (trans == null) {
			path.throwIllegalState("TranslatorFactory " + transFact + " was unable to produce a Translator for " + type);
			return null;	// never gets here
		} else {
			return trans;
		}
	}
}
