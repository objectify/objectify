/*
 */

package com.googlecode.objectify.test;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.test.util.TestBase;
import org.testng.annotations.Test;
import java.util.ArrayList;
import java.util.List;
import static com.googlecode.objectify.test.util.TestObjectifyService.fact;
import static com.googlecode.objectify.test.util.TestObjectifyService.ofy;

/**
 * Tests of basic query operations
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class QueryCollectionTests extends TestBase
{
	@Entity
	private static class WithCollection {
		@Id
		public Long id;

		@Index
		public List<String> stuff = new ArrayList<>();
	}

	/** */
	@Test
	public void queryForStringInList() throws Exception {
		fact().register(WithCollection.class);

		WithCollection wc = new WithCollection();
		wc.stuff.add("foo");
		ofy().save().entity(wc).now();

		ofy().clear();
		List<WithCollection> list = ofy().load().type(WithCollection.class).filter("stuff", "foo").list();

		assert list.size() == 1;
		assert list.get(0).id == wc.id;
	}
}