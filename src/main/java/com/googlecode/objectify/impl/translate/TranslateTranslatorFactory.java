package com.googlecode.objectify.impl.translate;

import com.googlecode.objectify.annotation.Translate;
import com.googlecode.objectify.impl.Path;


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
	public Translator<Object, Object> create(TypeKey<Object> tk, CreateContext ctx, Path path) {
		final Translate translateAnno = tk.getAnnotation(Translate.class);

		if (translateAnno == null)
			return null;

		if (earlyOnly && !translateAnno.early())
			return null;

		@SuppressWarnings("unchecked")
		TranslatorFactory<Object, Object> transFact = (TranslatorFactory<Object, Object>)ctx.getFactory().construct(translateAnno.value());

		Translator<Object, Object> trans = transFact.create(tk, ctx, path);
		if (trans == null) {
			path.throwIllegalState("TranslatorFactory " + transFact + " was unable to produce a Translator for " + tk.getType());
			return null;	// never gets here
		} else {
			return trans;
		}
	}
}
