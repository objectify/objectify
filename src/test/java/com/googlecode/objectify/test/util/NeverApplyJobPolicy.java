/*
 */

package com.googlecode.objectify.test.util;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.dev.HighRepJobPolicy;

/**
 * This guarantees eventual consistency from the HR datastore, but always grooms.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class NeverApplyJobPolicy implements HighRepJobPolicy
{
	@Override
	public boolean shouldApplyNewJob(Key arg0) {
		return false;
	}

	@Override
	public boolean shouldRollForwardExistingJob(Key arg0) {
		return true;
	}
}
