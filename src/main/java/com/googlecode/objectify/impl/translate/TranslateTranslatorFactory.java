package com.googlecode.objectify.impl.translate;

import java.lang.reflect.Type;

import com.googlecode.objectify.annotation.Translate;
import com.googlecode.objectify.impl.Path;
import com.googlecode.objectify.impl.Property;


/**
 * <p>Translator factory which lets users define a custom translator for a field.</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class TranslateTranslatorFactory implements TranslatorFactory<Object>
{
	boolean earlyOnly;
	
	/**
	 * @param earlyOnly determines whether this instance ignores @Translate annotations with early=false
	 */
	public TranslateTranslatorFactory(boolean earlyOnly) {
		this.earlyOnly = earlyOnly;
	}
	
	@Override
	public Translator<Object> create(Path path, Property property, Type type, final CreateContext ctx) {

		final Translate translateAnno = property.getAnnotation(Translate.class);
		
		if (translateAnno == null)
			return null;
		
		if (earlyOnly && !translateAnno.early())
			return null;

		@SuppressWarnings("unchecked")
		TranslatorFactory<Object> transFact = (TranslatorFactory<Object>)ctx.getFactory().construct(translateAnno.value());
		
		Translator<Object> trans = transFact.create(path, property, type, ctx);
		if (trans == null) {
			path.throwIllegalState("TranslatorFactory " + transFact + " was unable to produce a Translator for " + type);
			return null;	// never gets here
		} else {
			return trans;
		}
	}
}
