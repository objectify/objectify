/*
 */

package com.googlecode.objectify.test;

import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.FullEntity;
import com.google.cloud.datastore.NullValue;
import com.google.common.collect.Sets;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Ignore;
import com.googlecode.objectify.test.entity.Trivial;
import com.googlecode.objectify.test.util.TestBase;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import static com.google.common.truth.Truth.assertThat;
import static com.googlecode.objectify.ObjectifyService.factory;
import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * Tests of various collection types
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class CollectionTests extends TestBase {

	/** */
	private static class CustomSet extends HashSet<Integer> {
		private static final long serialVersionUID = 1L;

		public int tenTimesSize() {
			return this.size() * 10;
		}
	}

	/** */
	@com.googlecode.objectify.annotation.Entity
	@Cache
	private static class HasCollections {
		@Id
		private Long id;

		private List<Integer> integerList;
		private LinkedList<Integer> integerLinkedList;
		private ArrayList<Integer> integerArrayList;

		private Set<Integer> integerSet;
		private SortedSet<Integer> integerSortedSet;
		private HashSet<Integer> integerHashSet;
		private TreeSet<Integer> integerTreeSet;
		private LinkedHashSet<Integer> integerLinkedHashSet;

		private List<Integer> initializedList = new LinkedList<>();

		private CustomSet customSet;

		/**
		 * This should give the system a workout
		 */
		private Set<Key<Trivial>> typedKeySet;
	}

	/** */
	private void assertContains123(final Collection<Integer> coll, final Class<?> expectedClass) {
		assertThat(coll.getClass()).isEqualTo(expectedClass);
		assertThat(coll).containsExactly(1, 2, 3).inOrder();
	}

	/** */
	@Test
	void basicListsWork() throws Exception {
		factory().register(HasCollections.class);

		final HasCollections hc = new HasCollections();
		hc.integerList = Arrays.asList(1, 2, 3);
		hc.integerArrayList = new ArrayList<>(hc.integerList);
		hc.integerLinkedList = new LinkedList<>(hc.integerList);

		final HasCollections fetched = saveClearLoad(hc);

		assertContains123(fetched.integerList, ArrayList.class);
		assertContains123(fetched.integerArrayList, ArrayList.class);
		assertContains123(fetched.integerLinkedList, LinkedList.class);
	}

	/** */
	@Test
	void basicSetsWork() throws Exception {
		factory().register(HasCollections.class);

		final HasCollections hc = new HasCollections();
		hc.integerSet = Sets.newHashSet(1, 2, 3);
		hc.integerSortedSet = new TreeSet<>(hc.integerSet);
		hc.integerHashSet = new HashSet<>(hc.integerSet);
		hc.integerTreeSet = new TreeSet<>(hc.integerSet);
		hc.integerLinkedHashSet = new LinkedHashSet<>(hc.integerSet);

		final HasCollections fetched = saveClearLoad(hc);

		assertContains123(fetched.integerSet, HashSet.class);
		assertContains123(fetched.integerSortedSet, TreeSet.class);
		assertContains123(fetched.integerHashSet, HashSet.class);
		assertContains123(fetched.integerTreeSet, TreeSet.class);
		assertContains123(fetched.integerLinkedHashSet, LinkedHashSet.class);
	}

	/** */
	@Test
	void customCollectionTypesArePreserved() throws Exception {
		factory().register(HasCollections.class);

		final HasCollections hc = new HasCollections();
		hc.customSet = new CustomSet();
		hc.customSet.add(1);
		hc.customSet.add(2);
		hc.customSet.add(3);

		final HasCollections fetched = saveClearLoad(hc);

		assertContains123(fetched.customSet, CustomSet.class);
	}

	/** */
	@Test
	void keySetPreservesValues() throws Exception {
		factory().register(HasCollections.class);

		final Key<Trivial> key7 = Key.create(Trivial.class, 7);
		final Key<Trivial> key8 = Key.create(Trivial.class, 8);
		final Key<Trivial> key9 = Key.create(Trivial.class, 9);

		final HasCollections hc = new HasCollections();
		hc.typedKeySet = new HashSet<>(Arrays.asList(key7, key8, key9));

		final HasCollections fetched = saveClearLoad(hc);

		assertThat(fetched.typedKeySet).isInstanceOf(HashSet.class);
		assertThat(fetched.typedKeySet).containsExactly(key7, key8, key9);	// order not relevant
	}

	@Test
	void nullsInCollectionsArePreserved() throws Exception {
		factory().register(HasCollections.class);

		final HasCollections hc = new HasCollections();
		hc.integerList = Arrays.asList((Integer) null);

		final HasCollections fetched = saveClearLoad(hc);

		assertThat(fetched.integerList).containsExactly((Integer)null);
	}

	/**
	 */
	@Test
	void nullCollectionsAreLeftAlone() throws Exception {
		factory().register(HasCollections.class);

		final HasCollections hc = new HasCollections();

		final HasCollections fetched = saveClearLoad(hc);
		assertThat(fetched.integerList).isNull();

		final Entity e = datastore().get(Key.create(fetched).getRaw());
		// rule : never store a null collection
		assertThat(e.getNames()).doesNotContain("integerList");
	}

	/**
	 */
	@Test
	void emptyCollectionsAreSameAsNullCollections() throws Exception {
		factory().register(HasCollections.class);

		final HasCollections hc = new HasCollections();
		hc.integerList = new ArrayList<>();

		final HasCollections fetched = saveClearLoad(hc);
		assertThat(fetched.integerList).isNull();

		Entity e = datastore().get(Key.create(fetched).getRaw());
		// rule : never store an empty collection
		assertThat(e.getNames()).doesNotContain("integerList");

		// The initialized one stays initialized
		assertThat(fetched.initializedList).isNotNull();
		assertThat(fetched.initializedList).isInstanceOf(LinkedList.class);
	}

	/**
	 */
	@Test
	void nullValuesInDatastoreAreIgnored() throws Exception {
		factory().register(HasCollections.class);

		final FullEntity<?> ent = makeEntity(HasCollections.class)
				.set("integerList", NullValue.of())
				.set("initializedList", NullValue.of())
				.build();

		final Entity e = datastore().put(ent);

		final HasCollections fetched = (HasCollections)ofy().load().value(e.getKey()).now();
		assertThat(fetched.integerList).isNull();
		assertThat(fetched.initializedList).isEmpty();
	}

	/** */
	@com.googlecode.objectify.annotation.Entity
	private static class HasInitializedCollection {
		@Id
		private Long id;
		private List<String> initialized = new ArrayList<>();
		@Ignore
		private List<String> copyOf;

		public HasInitializedCollection() {
			this.copyOf = initialized;
		}
	}

	/**
	 */
	@Test
	void doNotOverwriteAnAlreadyInitializedConcreteCollection() throws Exception {
		factory().register(HasInitializedCollection.class);

		final HasInitializedCollection has = new HasInitializedCollection();
		HasInitializedCollection fetched = saveClearLoad(has);
		assertThat(fetched.initialized).isSameInstanceAs(fetched.copyOf);
	}

	/**
	 */
	@Test
	void doNotOverwriteAnAlreadyInitializedConcreteCollection2() throws Exception {
		factory().register(HasInitializedCollection.class);

		final HasInitializedCollection has = new HasInitializedCollection();
		has.initialized.add("blah");
		HasInitializedCollection fetched = saveClearLoad(has);
		assertThat(fetched.initialized).isSameInstanceAs(fetched.copyOf);
	}

	/**
	 * Without the generic type
	 */
	@com.googlecode.objectify.annotation.Entity
	@SuppressWarnings("rawtypes")
	private static class HasRawCollection {
		@Id
		private Long id;
		private Set raw = new HashSet();
	}

	@Test
	@SuppressWarnings("unchecked")
	void rawSetsArePersisted() {
		factory().register(HasRawCollection.class);

		final HasRawCollection hrc = new HasRawCollection();
		hrc.raw.add("foo");

		final HasRawCollection fetched = saveClearLoad(hrc);

		assertThat(fetched.raw).isEqualTo(hrc.raw);
	}

	@com.googlecode.objectify.annotation.Entity
	private static class HasStringList {
		@Id
		private Long id;
		private List<String> list = new ArrayList<>();
	}

	@Test
	void stringListWorks() {
		factory().register(HasStringList.class);

		final HasStringList h = new HasStringList();
		h.list.add("foo");
		h.list.add("bar");

		final HasStringList fetched = saveClearLoad(h);

		assertThat(fetched.list).isEqualTo(h.list);
	}
}
