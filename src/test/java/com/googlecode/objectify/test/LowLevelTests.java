/*
 */

package com.googlecode.objectify.test;

import com.google.appengine.api.datastore.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.test.util.TestBase;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.testng.annotations.Test;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static com.googlecode.objectify.test.util.TestObjectifyService.fact;

/**
 * Just verifying some low-level api behavior.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class LowLevelTests extends TestBase
{
	/**
	 * throws java.lang.IllegalArgumentException: lol: java.util.Arrays$ArrayList is not a supported property type.
	 */
	@Test(expectedExceptions = IllegalArgumentException.class)
	public void listOfLists() throws Exception {
		List<List<String>> lol = new ArrayList<>(Arrays.asList(Arrays.asList("one", "two"), Arrays.asList("three", "four")));

		Entity ent = new Entity("thing");
		ent.setUnindexedProperty("lol", lol);
		ds().put(ent);

		Entity fetched = ds().get(ent.getKey());

		assert fetched.getProperty("lol").equals(lol);
	}

	/** */
	@com.googlecode.objectify.annotation.Entity
	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class Thing {
		@Id private Long id;
		private List<List<String>> lol;
	}

	/**
	 * Blows up on an internal assertion in Objectify
	 */
	@Test(expectedExceptions = AssertionError.class)
	public void listOfListsWithObjectify() throws Exception {
		fact().register(Thing.class);
		List<List<String>> lol = new ArrayList<>(Arrays.asList(Arrays.asList("one", "two"), Arrays.asList("three", "four")));

		Thing th = new Thing(null, lol);

		Thing fetched = this.putClearGet(th);

		assert fetched.lol.equals(lol);
	}
}