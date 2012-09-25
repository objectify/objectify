package com.googlecode.objectify.impl.translate;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.impl.Node;
import com.googlecode.objectify.impl.Path;
import com.googlecode.objectify.impl.Property;
import com.googlecode.objectify.impl.TranslatableProperty;
import com.googlecode.objectify.impl.TypeUtils;
import com.googlecode.objectify.util.LogUtils;

/**
 * Translator which knows what to do with a whole class.  This is used by the EmbedClassTranslatorFactory and
 * also subclassed to produce a RootClassTranslator, which is managed specially.
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class ClassTranslator<T> extends MapNodeTranslator<T>
{
	private static final Logger log = Logger.getLogger(ClassTranslator.class.getName());
	
	/** */
	protected final ObjectifyFactory fact;
	protected final Class<T> clazz;
	protected final List<TranslatableProperty<Object>> props = new ArrayList<TranslatableProperty<Object>>();

	/** */
	public ClassTranslator(Class<T> clazz, Path path, CreateContext ctx)
	{
		this.fact = ctx.getFactory();
		this.clazz = clazz;

		if (log.isLoggable(Level.FINEST))
			log.finest("Creating class translator for " + clazz.getName() + " at path '"+ path + "'");
		
		// Quick sanity check - can we construct one of these?  If not, blow up.
		try {
			fact.construct(clazz);
		} catch (Exception ex) {
			throw new IllegalStateException("Unable to construct an instance of " + clazz.getName() + "; perhaps it has no suitable constructor?", ex);
		}
		
		for (Property prop: TypeUtils.getProperties(fact, clazz)) {
			Path propPath = path.extend(prop.getName());
			Translator<Object> loader = fact.getTranslators().create(propPath, prop, prop.getType(), ctx);
			TranslatableProperty<Object> tprop = new TranslatableProperty<Object>(prop, loader);
			props.add(tprop);
			
			// Sanity check here
			if (prop.hasIgnoreSaveConditions() && ctx.isInCollection() && ctx.isInEmbed())	// of course we're in embed
				propPath.throwIllegalState("You cannot use conditional @IgnoreSave within @Embed collections. @IgnoreSave is only allowed without conditions.");
			
			this.foundTranslatableProperty(tprop);
		}
	}
	
	/** */
	public Class<?> getTranslatedClass() { return this.clazz; }
	
	/** 
	 * Called when each property is discovered, allows a subclass to do something special with it
	 */
	protected void foundTranslatableProperty(TranslatableProperty<Object> tprop) {}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.translate.MapNodeTranslator#loadMap(com.googlecode.objectify.impl.Node, com.googlecode.objectify.impl.translate.LoadContext)
	 */
	@Override
	protected T loadMap(Node node, LoadContext ctx) {
		if (log.isLoggable(Level.FINEST))
			log.finest(LogUtils.msg(node.getPath(), "Instantiating a " + clazz.getName()));
			
		T pojo = fact.construct(clazz);
		
		for (TranslatableProperty<Object> prop: props) {
			prop.executeLoad(node, pojo, ctx);
		}
		
		return pojo;
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.translate.MapNodeTranslator#saveMap(java.lang.Object, com.googlecode.objectify.impl.Path, boolean, com.googlecode.objectify.impl.translate.SaveContext)
	 */
	@Override
	protected Node saveMap(T pojo, Path path, boolean index, SaveContext ctx) {
		Node node = new Node(path);
		
		for (TranslatableProperty<Object> prop: props)
			prop.executeSave(pojo, node, index, ctx);
		
		return node;
	}
}