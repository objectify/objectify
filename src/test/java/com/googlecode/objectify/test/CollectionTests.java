/*
 */

package com.googlecode.objectify.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Logger;

import org.testng.annotations.Test;

import com.google.appengine.api.datastore.Entity;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Ignore;
import com.googlecode.objectify.impl.Keys;
import com.googlecode.objectify.test.entity.Trivial;
import com.googlecode.objectify.test.util.TestBase;
import com.googlecode.objectify.test.util.TestObjectify;

/**
 * Tests of various collection types
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class CollectionTests extends TestBase
{
	/** */
	@SuppressWarnings("unused")
	private static Logger log = Logger.getLogger(CollectionTests.class.getName());

	/** */
	public static class CustomSet extends HashSet<Integer>
	{
		private static final long serialVersionUID = 1L;
		public int tenTimesSize() { return this.size() * 10; }
	}

	/** */
	@com.googlecode.objectify.annotation.Entity
	@Cache
	static class HasCollections
	{
		public @Id Long id;

		public List<Integer> integerList;
		public LinkedList<Integer> integerLinkedList;
		public ArrayList<Integer> integerArrayList;

		public Set<Integer> integerSet;
		public SortedSet<Integer> integerSortedSet;
		public HashSet<Integer> integerHashSet;
		public TreeSet<Integer> integerTreeSet;
		public LinkedHashSet<Integer> integerLinkedHashSet;

		public List<Integer> initializedList = new LinkedList<Integer>();

		public CustomSet customSet;

		/** This should give the system a workout */
		public Set<Key<Trivial>> typedKeySet;
	}

	/** */
	private void assertContains123(Collection<Integer> coll, Class<?> expectedClass)
	{
		assert coll.getClass() == expectedClass;	// will fail with caching objectify, this is ok

		assert coll.size() == 3;
		Iterator<Integer> it = coll.iterator();
		assert it.next() == 1;
		assert it.next() == 2;
		assert it.next() == 3;
	}

	/** */
	@Test
	public void testBasicLists() throws Exception
	{
		fact.register(HasCollections.class);
		TestObjectify ofy = this.fact.begin();

		HasCollections hc = new HasCollections();
		hc.integerList = Arrays.asList(1, 2, 3);

		hc.integerArrayList = new ArrayList<Integer>(hc.integerList);
		hc.integerLinkedList = new LinkedList<Integer>(hc.integerList);

		Key<HasCollections> key = ofy.save().entity(hc).now();
		ofy.clear();
		hc = ofy.load().key(key).get();

		assertContains123(hc.integerList, ArrayList.class);
		assertContains123(hc.integerArrayList, ArrayList.class);
		assertContains123(hc.integerLinkedList, LinkedList.class);
	}

	/** */
	@Test
	public void testBasicSets() throws Exception
	{
		fact.register(HasCollections.class);
		TestObjectify ofy = this.fact.begin();

		HasCollections hc = new HasCollections();
		hc.integerSet = new HashSet<Integer>();
		hc.integerSet.add(1);
		hc.integerSet.add(2);
		hc.integerSet.add(3);

		hc.integerSortedSet = new TreeSet<Integer>(hc.integerSet);
		hc.integerHashSet = new HashSet<Integer>(hc.integerSet);
		hc.integerTreeSet = new TreeSet<Integer>(hc.integerSet);
		hc.integerLinkedHashSet = new LinkedHashSet<Integer>(hc.integerSet);

		Key<HasCollections> key = ofy.save().entity(hc).now();
		hc = ofy.load().key(key).get();

		assertContains123(hc.integerSet, HashSet.class);
		assertContains123(hc.integerSortedSet, TreeSet.class);
		assertContains123(hc.integerHashSet, HashSet.class);
		assertContains123(hc.integerTreeSet, TreeSet.class);
		assertContains123(hc.integerLinkedHashSet, LinkedHashSet.class);
	}

	/** */
	@Test
	public void testCustomSet() throws Exception
	{
		fact.register(HasCollections.class);
		TestObjectify ofy = this.fact.begin();

		HasCollections hc = new HasCollections();
		hc.customSet = new CustomSet();
		hc.customSet.add(1);
		hc.customSet.add(2);
		hc.customSet.add(3);

		Key<HasCollections> key = ofy.save().entity(hc).now();
		hc = ofy.load().key(key).get();

		assertContains123(hc.customSet, CustomSet.class);
	}

	/** */
	@Test
	public void testTypedKeySet() throws Exception
	{
		fact.register(HasCollections.class);
		TestObjectify ofy = this.fact.begin();

		Key<Trivial> key7 = Key.create(Trivial.class, 7);
		Key<Trivial> key8 = Key.create(Trivial.class, 8);
		Key<Trivial> key9 = Key.create(Trivial.class, 9);

		HasCollections hc = new HasCollections();
		hc.typedKeySet = new HashSet<Key<Trivial>>();
		hc.typedKeySet.add(key7);
		hc.typedKeySet.add(key8);
		hc.typedKeySet.add(key9);

		Key<HasCollections> key = ofy.save().entity(hc).now();
		hc = ofy.load().key(key).get();

		assert hc.typedKeySet instanceof HashSet<?>;
		assert hc.typedKeySet.size() == 3;

		assert hc.typedKeySet.contains(key7);
		assert hc.typedKeySet.contains(key8);
		assert hc.typedKeySet.contains(key9);
	}

	@Test
	public void testCollectionContainingNull() throws Exception
	{
		fact.register(HasCollections.class);
		TestObjectify ofy = this.fact.begin();

		HasCollections hc = new HasCollections();
		hc.integerList = Arrays.asList((Integer) null);

		Key<HasCollections> key = ofy.save().entity(hc).now();
		hc = ofy.load().key(key).get();

		assert hc.integerList != null;
		assert hc.integerList.size() == 1;
		assert hc.integerList.get(0) == null;

		Entity e = ds().get(Keys.toRawKey(key));
		assert e.hasProperty("integerList");
		List<?> l = (List<?>) e.getProperty("integerList");
		assert l != null;
		assert l.size() == 1;
		assert l.get(0) == null;
	}

	/**
	 * Rule: never store a null Collection, always leave it alone when loaded
	 */
	@Test
	public void testNullCollections() throws Exception
	{
		fact.register(HasCollections.class);
		TestObjectify ofy = this.fact.begin();

		HasCollections hc = new HasCollections();
		hc.integerList = null;

		Key<HasCollections> key = ofy.save().entity(hc).now();
		hc = ofy.load().key(key).get();

		ofy.save().entity(hc).now();
		hc = ofy.load().key(key).get();
		assert hc.integerList == null;	// not loaded

		Entity e = ds().get(Keys.toRawKey(key));
		// rule : never store a null collection
		assert !e.hasProperty("integerList");
	}

	/**
	 * Test rule: never store an empty Collection, leaves value as null
	 */
	@Test
	public void testEmptyCollections() throws Exception
	{
		fact.register(HasCollections.class);
		TestObjectify ofy = this.fact.begin();

		HasCollections hc = new HasCollections();
		hc.integerList = new ArrayList<Integer>();

		Key<HasCollections> key = ofy.save().entity(hc).now();
		ofy.clear();
		hc = ofy.load().key(key).get();

		System.out.println(ds().get(Keys.toRawKey(hc)));

		// This wouldn't be valid if we didn't clear the session
		assert hc.integerList == null;

		Entity e = ds().get(key.getRaw());
		// rule : never store an empty collection
		assert !e.hasProperty("integerList");

		assert hc.initializedList != null;
		assert hc.initializedList instanceof LinkedList<?>;
	}

	/** */
	@com.googlecode.objectify.annotation.Entity
	public static class HasInitializedCollection
	{
		public @Id Long id;
		public List<String> initialized = new ArrayList<String>();
		@Ignore public List<String> copyOf;

		public HasInitializedCollection()
		{
			this.copyOf = initialized;
		}
	}

	/**
	 * Make sure that Objectify doesn't overwrite an already initialized concrete collection
	 */
	@Test
	public void testInitializedCollections() throws Exception
	{
		this.fact.register(HasInitializedCollection.class);

		HasInitializedCollection has = new HasInitializedCollection();
		HasInitializedCollection fetched = this.putClearGet(has);
		assert fetched.initialized == fetched.copyOf;	// should be same object

		has = new HasInitializedCollection();
		has.initialized.add("blah");
		fetched = this.putClearGet(has);
		assert fetched.initialized == fetched.copyOf;	// should be same object
	}

	/**
	 * Without the generic type
	 */
	@com.googlecode.objectify.annotation.Entity
	@SuppressWarnings("rawtypes")
	public static class HasRawCollection
	{
		@Id Long id;
		Set raw = new HashSet();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testRawtypeSet()
	{
		this.fact.register(HasRawCollection.class);

		HasRawCollection hrc = new HasRawCollection();
		hrc.raw.add("foo");

		HasRawCollection fetched = this.putClearGet(hrc);

		assert hrc.raw.equals(fetched.raw);
	}
}
