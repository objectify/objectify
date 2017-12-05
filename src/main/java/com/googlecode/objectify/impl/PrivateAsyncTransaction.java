package com.googlecode.objectify.impl;

import com.googlecode.objectify.Result;

/**
 * A private interface for manipulating transactions. This is a (hopefully) temporary hack that is not exposed
 * in the API.
 */
public interface PrivateAsyncTransaction extends AsyncTransaction {

	void runCommitListeners();

	void enlist(final Result<?> result);
}