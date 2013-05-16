/*
 */

package com.googlecode.objectify.test;

import static com.googlecode.objectify.test.util.TestObjectifyService.fact;
import static com.googlecode.objectify.test.util.TestObjectifyService.ofy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.test.entity.Trivial;
import com.googlecode.objectify.test.util.TestBaseInconsistent;

/**
 * Tests of queries when they are eventual
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class QueryEventualityTests extends TestBaseInconsistent
{
	/** */
	@SuppressWarnings("unused")
	private static Logger log = Logger.getLogger(QueryEventualityTests.class.getName());

	/** */
	Trivial triv1;
	Trivial triv2;
	List<Key<Trivial>> keys;

	/** */
	@BeforeMethod
	public void setUp() {
		super.setUp();

		fact().register(Trivial.class);

		this.triv1 = new Trivial("foo1", 1);
		this.triv2 = new Trivial("foo2", 2);

		List<Trivial> trivs = new ArrayList<Trivial>();
		trivs.add(this.triv1);
		trivs.add(this.triv2);

		Map<Key<Trivial>, Trivial> result = ofy().save().entities(trivs).now();

		this.keys = new ArrayList<Key<Trivial>>(result.keySet());
	}

	/**
	 * Delete creates a negative cache result, so when the value comes back we should not insert a null
	 * but rather pretend the value does not exist.
	 */
	@Test
	public void deleteWorks() throws Exception {
		ofy().delete().entity(triv1).now();

		List<Trivial> found = ofy().load().type(Trivial.class).list();
		assert found.size() == 1;
		assert found.get(0).getId().equals(triv2.getId());
	}

	/**
	 * Delete creates a negative cache result, so when the value comes back we should not insert a null
	 * but rather pretend the value does not exist.
	 */
	@Test
	public void deleteAllWorks() throws Exception {
		ofy().delete().entities(triv1, triv2).now();

		List<Trivial> found = ofy().load().type(Trivial.class).list();
		assert found.isEmpty();
	}
}
