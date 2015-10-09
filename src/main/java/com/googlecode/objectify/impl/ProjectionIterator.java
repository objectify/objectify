package com.googlecode.objectify.impl;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.QueryResultIterator;
import com.googlecode.objectify.impl.translate.LoadContext;
import com.googlecode.objectify.util.TranslatingQueryResultIterator;

import java.util.logging.Logger;

/**
 * Projections bypass the session entirely, neither loading nor saving.
 */
public class ProjectionIterator<T> extends TranslatingQueryResultIterator<Entity, T> {
	/** */
	private static final Logger log = Logger.getLogger(ProjectionIterator.class.getName());

	private final LoadEngine loadEngine;
	private final LoadContext ctx;

	/** */
	public ProjectionIterator(QueryResultIterator<Entity> base, LoadEngine loadEngine) {
		super(base);
		this.loadEngine = loadEngine;
		this.ctx = new LoadContext(loadEngine);
	}

	@Override
	protected T translate(Entity from) {
		return loadEngine.load(from, ctx);
	}
}
