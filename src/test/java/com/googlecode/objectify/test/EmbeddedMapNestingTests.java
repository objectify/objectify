package com.googlecode.objectify.test;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.test.util.TestBase;

import java.util.HashMap;
import java.util.Map;

import static com.google.common.truth.Truth.assertThat;
import static com.googlecode.objectify.ObjectifyService.factory;

/**
 */
class EmbeddedMapNestingTests extends TestBase
{
	@Entity
	private static class HasMap {
		@Id
		Long id;
		Map<String, Object> map = new HashMap<>();
	}

	/**
	 * Testing issue #347
	 * This is not actually possible; because the type of the value is Object, the FullEntity that comes back from
	 * the datastore will be left as-is. Objectify can't know that you want it to be turned into a Map.
	 * Disabling the test but leaving it here in case we want to add some extra annotations to enable this
	 * behavior in the future.
	 */
	//@Test
	void canNestMaps() throws Exception {
		factory().register(HasMap.class);

		final HasMap hm = new HasMap();

		final Map<String, Object> map = hm.map;
		final Map<String, Object> map2 = new HashMap<>();
		final Map<String, Object> map3 = new HashMap<>();
		map.put("level", 1);
		map2.put("level", 2);
		map3.put("level", 3);
		map3.put("val", "alldone");
		map2.put("embed", map3);
		map.put("embed", map2);

		final HasMap fetched = saveClearLoad(hm);

		assertThat(fetched.map).isEqualTo(hm.map);
	}
}
