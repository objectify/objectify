package com.googlecode.objectify.test;

import static com.googlecode.objectify.ObjectifyService.factory;
import static com.googlecode.objectify.ObjectifyService.ofy;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.OnSave;
import com.googlecode.objectify.impl.WriteEngine;
import com.googlecode.objectify.test.util.TestBase;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

import org.testng.annotations.Test;

public class SaveTests extends TestBase {

	/**
	 * The {@link WriteEngine} previously iterated twice on the {@link Iterables}
	 * passed as argument. If the iterable doesn't return the same set of entities
	 * upon the second iteration, it can lead to problems.
	 * 
	 * This can easily happen if there's a filtering iterable that uses a predicate
	 * relying on a property changed by an {@link OnSave} hook.
	 */
	@Test
	public void testSaveChangingIterable() {
		factory().register(EntityWithOnSave.class);
		List<EntityWithOnSave> entities = Lists.newArrayList(new EntityWithOnSave(1L, null));
		Iterable<EntityWithOnSave> entitiesWithoutDefaultValue = Iterables.filter(entities,
				new Predicate<EntityWithOnSave>() {
					@Override
					public boolean apply(EntityWithOnSave entity) {
						return entity.getSomeValue() == null;
					}
				});
		Map<Key<EntityWithOnSave>, EntityWithOnSave> saveResult = ofy().save().entities(entitiesWithoutDefaultValue)
				.now();
		assert 1 == saveResult.size();
	}

	@Entity
	@Cache
	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class EntityWithOnSave {
		@Id
		private Long id;

		private String someValue;

		@OnSave
		public void onSave() {
			if (someValue == null) {
				someValue = "some calculated default value";
			}
		}
	}
}
