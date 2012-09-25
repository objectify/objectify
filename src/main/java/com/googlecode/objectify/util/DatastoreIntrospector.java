/*
 */

package com.googlecode.objectify.util;

import com.google.appengine.api.datastore.DatastoreAttributes.DatastoreType;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.TransactionOptions;
import com.google.appengine.api.utils.SystemProperty;



/**
 * <p>Lets us probe for certain datastore capabilities which may vary depending on dev/production/ms/hrd/etc</p>
 * 
 * @author Jeff Schnitzer
 */
public class DatastoreIntrospector
{
	/** true if XG transactions are supported */
	public static final boolean SUPPORTS_XG;
	static {
		// This is convoluted.  In production, we can check the DatastoreAttributes to see if we are on HRD.
		// But that doesn't work in development mode.  So in that case, we actually try an XG transaction and
		// see if it fails.
		DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
		
		if (SystemProperty.environment.value() == SystemProperty.Environment.Value.Production) {
			SUPPORTS_XG = ds.getDatastoreAttributes().getDatastoreType().equals(DatastoreType.HIGH_REPLICATION);
		} else {
			boolean supports = false;
			try {
				ds.beginTransaction(TransactionOptions.Builder.withXG(true)).rollback();
				supports = true;
			} catch (Exception ex) {
			} finally {
				SUPPORTS_XG = supports;
			}
		}
	}
}