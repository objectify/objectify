package com.googlecode.objectify.impl;

import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.impl.translate.CreateContext;
import com.googlecode.objectify.impl.translate.LoadContext;
import com.googlecode.objectify.impl.translate.SaveContext;
import com.googlecode.objectify.impl.translate.Translator;

/**
 * Enhanced TranslateProperty used for key fields  
 */
public class TranslateKeyProperty<P, D> extends TranslateProperty<P>
{
	private static final SaveContext NULL_SAVE_CONTEXT = new SaveContext(null);
	
	/** */
	@SuppressWarnings("unchecked")
	public TranslateKeyProperty(ObjectifyFactory fact, Property field) {
		super(field,
			(Translator<P>)fact.getTranslators().create(Path.of(field.getName()), field.getAnnotations(), field.getType(), new CreateContext(fact)));
	}
	
	/**
	 * Executes setting the value on the appropriate property of the pojo
	 */
	public void executeLoad(D value, Object pojo, LoadContext ctx) {
		this.executeLoad(Node.of(value), pojo, ctx);
	}
	
	/**
	 * Translates a pojo representation to a datastore representation
	 */
	@SuppressWarnings("unchecked")
	public D save(P value) {
		return (D)translator.save(value, Path.root(), false, NULL_SAVE_CONTEXT).getPropertyValue();
	}
}
