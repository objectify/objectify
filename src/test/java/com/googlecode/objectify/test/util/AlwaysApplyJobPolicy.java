/*
 */

package com.googlecode.objectify.test.util;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.dev.HighRepJobPolicy;

/**
 * This eliminates eventual consistency from the HR datastore, necessary for sane testing.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class AlwaysApplyJobPolicy implements HighRepJobPolicy
{
	@Override
	public boolean shouldApplyNewJob(Key arg0) {
		return true;
	}

	@Override
	public boolean shouldRollForwardExistingJob(Key arg0) {
		// This should be irrelevant because all jobs apply immediately
		return true;
	}
}
