package com.googlecode.objectify.test;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.test.util.TestBase;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.google.common.truth.Truth.assertThat;
import static com.googlecode.objectify.ObjectifyService.factory;

/**
 */
class EmbeddedCollectionTests extends TestBase {

	@Entity
	@Data
	private static class HasSet {
		@Id private Long id;
		private Set<HashableThing> someSet = new HashSet<>();
	}

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	private static class HashableThing {
		private Integer value;
	}

	@Test
	void setOfEmbeddedWorks() throws Exception {
		factory().register(HasSet.class);

		final HasSet has = new HasSet();
		has.someSet.add(new HashableThing(4));
		has.someSet.add(new HashableThing(5));
		has.someSet.add(new HashableThing(6));

		final HasSet fetched = saveClearLoad(has);

		assertThat(fetched.someSet).isEqualTo(has.someSet);
	}

	@Test
	void embeddedNullsWork() throws Exception {
		factory().register(HasSet.class);

		final HasSet has = new HasSet();
		has.someSet.add(null);

		final HasSet fetched = saveClearLoad(has);

		assertThat(fetched.someSet).isEqualTo(has.someSet);
	}

	/** Has an embed class in an embed collection */
	@Entity
	@Data
	private static class HasDeepThings {
		@Id
		private Long id;
		private List<DeepThing> deeps = new ArrayList<>();
	}

	/** */
	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	private static class DeepThing {
		private HashableThing thing;
	}

	/** */
	@Test
	void testHasDeepThings() throws Exception {
		factory().register(HasDeepThings.class);

		final HasDeepThings has = new HasDeepThings();
		has.deeps.add(new DeepThing(new HashableThing(4)));
		has.deeps.add(new DeepThing(new HashableThing(5)));

		final HasDeepThings fetched = saveClearLoad(has);

		assertThat(fetched.deeps).isEqualTo(has.deeps);
	}
}
