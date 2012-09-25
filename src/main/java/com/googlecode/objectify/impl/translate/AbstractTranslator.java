package com.googlecode.objectify.impl.translate;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.googlecode.objectify.impl.Node;
import com.googlecode.objectify.impl.Path;
import com.googlecode.objectify.util.LogUtils;

/**
 * <p>Very simple helper for all kinds of translators.</p>
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
abstract public class AbstractTranslator<T> implements Translator<T>
{
	/** */
	private static final Logger log = Logger.getLogger(AbstractTranslator.class.getName());

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.translate.Translator#load(com.googlecode.objectify.impl.Node, com.googlecode.objectify.impl.translate.LoadContext)
	 */
	@Override
	final public T load(Node node, LoadContext ctx) {
		T obj = this.loadAbstract(node, ctx);

		if (log.isLoggable(Level.FINEST))
			log.logp(Level.FINEST, this.getClass().getName(), "load", LogUtils.msg(node.getPath(), "Loaded " + node + " to " + obj));

		return obj;
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.translate.Translator#save(java.lang.Object, com.googlecode.objectify.impl.Path, boolean, com.googlecode.objectify.impl.translate.SaveContext)
	 */
	@Override
	final public Node save(T pojo, Path path, boolean index, SaveContext ctx) {
		Node n = this.saveAbstract(pojo, path, index, ctx);

		if (log.isLoggable(Level.FINEST))
			log.logp(Level.FINEST, this.getClass().getName(), "save", LogUtils.msg(path, "Saved " + pojo + " to " + n));

		return n;
	}

	/**
	 * Implement loading
	 */
	abstract protected T loadAbstract(Node node, LoadContext ctx);

	/**
	 * Implement saving
	 */
	abstract protected Node saveAbstract(T pojo, Path path, boolean index, SaveContext ctx);
}
