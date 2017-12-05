package com.googlecode.objectify.impl;

import com.google.cloud.datastore.ProjectionEntity;
import com.google.cloud.datastore.QueryResults;
import com.googlecode.objectify.impl.translate.LoadContext;
import com.googlecode.objectify.util.TranslatingQueryResults;

/**
 * Projections bypass the session entirely, neither loading nor saving.
 */
class ProjectionQueryResults<T> extends TranslatingQueryResults<ProjectionEntity, T> {
	private final LoadEngine loadEngine;
	private final LoadContext ctx;

	/** */
	ProjectionQueryResults(final QueryResults<ProjectionEntity> base, final LoadEngine loadEngine) {
		super(base);
		this.loadEngine = loadEngine;
		this.ctx = new LoadContext(loadEngine);
	}

	@Override
	protected T translate(final ProjectionEntity from) {
		return loadEngine.load(from, ctx);
	}
}