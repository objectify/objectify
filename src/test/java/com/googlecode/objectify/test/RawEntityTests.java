/*
 */

package com.googlecode.objectify.test;

import com.google.appengine.api.datastore.Entity;
import com.googlecode.objectify.test.util.TestBase;
import org.testng.annotations.Test;
import java.util.logging.Logger;
import static com.googlecode.objectify.test.util.TestObjectifyService.ofy;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

/**
 * Tests of using the native datastore Entity type
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class RawEntityTests extends TestBase
{
	/** */
	@SuppressWarnings("unused")
	private static Logger log = Logger.getLogger(RawEntityTests.class.getName());

	/** */
	@Test
	public void saveAndLoadRawEntityWorks() throws Exception {
		Entity ent = new Entity("asdf");
		ent.setProperty("foo", "bar");

		ofy().save().entity(ent).now();
		ofy().clear();

		Entity fetched = ofy().load().<Entity>value(ent).now();

		assertThat(fetched.getProperty("foo"), equalTo(ent.getProperty("foo")));
	}

	/** */
	@Test
	public void deleteRawEntityWorks() throws Exception {
		Entity ent = new Entity("asdf");
		ent.setProperty("foo", "bar");

		ofy().save().entity(ent).now();
		ofy().clear();

		ofy().delete().entity(ent);

		Entity fetched = ofy().load().<Entity>value(ent).now();

		assertThat(fetched, nullValue());
	}
}
