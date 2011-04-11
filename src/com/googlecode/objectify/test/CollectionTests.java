/*
 * $Id: BeanMixin.java 1075 2009-05-07 06:41:19Z lhoriman $
 * $URL: https://subetha.googlecode.com/svn/branches/resin/rtest/src/org/subethamail/rtest/util/BeanMixin.java $
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
import java.util.TreeSet;
import java.util.logging.Logger;

import javax.persistence.Id;
import javax.persistence.Transient;

import org.testng.annotations.Test;

import com.google.appengine.api.datastore.Entity;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.test.entity.HasCollections;
import com.googlecode.objectify.test.entity.HasCollections.CustomSet;
import com.googlecode.objectify.test.entity.Trivial;

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
		Objectify ofy = this.fact.begin();

		HasCollections hc = new HasCollections();
		hc.integerList = Arrays.asList(1, 2, 3);

		hc.integerArrayList = new ArrayList<Integer>(hc.integerList);
		hc.integerLinkedList = new LinkedList<Integer>(hc.integerList);

		Key<HasCollections> key = ofy.put(hc);
		hc = ofy.get(key);

		assertContains123(hc.integerList, ArrayList.class);
		assertContains123(hc.integerArrayList, ArrayList.class);
		assertContains123(hc.integerLinkedList, LinkedList.class);
	}

	/** */
	@Test
	public void testBasicSets() throws Exception
	{
		Objectify ofy = this.fact.begin();

		HasCollections hc = new HasCollections();
		hc.integerSet = new HashSet<Integer>();
		hc.integerSet.add(1);
		hc.integerSet.add(2);
		hc.integerSet.add(3);

		hc.integerSortedSet = new TreeSet<Integer>(hc.integerSet);
		hc.integerHashSet = new HashSet<Integer>(hc.integerSet);
		hc.integerTreeSet = new TreeSet<Integer>(hc.integerSet);
		hc.integerLinkedHashSet = new LinkedHashSet<Integer>(hc.integerSet);

		Key<HasCollections> key = ofy.put(hc);
		hc = ofy.get(key);

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
		Objectify ofy = this.fact.begin();

		HasCollections hc = new HasCollections();
		hc.customSet = new CustomSet();
		hc.customSet.add(1);
		hc.customSet.add(2);
		hc.customSet.add(3);

		Key<HasCollections> key = ofy.put(hc);
		hc = ofy.get(key);

		assertContains123(hc.customSet, CustomSet.class);
	}

	/** */
	@Test
	public void testTypedKeySet() throws Exception
	{
		Objectify ofy = this.fact.begin();

		Key<Trivial> key7 = new Key<Trivial>(Trivial.class, 7);
		Key<Trivial> key8 = new Key<Trivial>(Trivial.class, 8);
		Key<Trivial> key9 = new Key<Trivial>(Trivial.class, 9);

		HasCollections hc = new HasCollections();
		hc.typedKeySet = new HashSet<Key<Trivial>>();
		hc.typedKeySet.add(key7);
		hc.typedKeySet.add(key8);
		hc.typedKeySet.add(key9);

		Key<HasCollections> key = ofy.put(hc);
		hc = ofy.get(key);

		assert hc.typedKeySet instanceof HashSet<?>;
		assert hc.typedKeySet.size() == 3;

		assert hc.typedKeySet.contains(key7);
		assert hc.typedKeySet.contains(key8);
		assert hc.typedKeySet.contains(key9);
	}

	@Test
	public void testCollectionContainingNull() throws Exception
	{
		Objectify ofy = this.fact.begin();

		HasCollections hc = new HasCollections();
		hc.integerList = Arrays.asList((Integer) null);

		Key<HasCollections> key = ofy.put(hc);
		hc = ofy.get(key);

		assert hc.integerList != null;
		assert hc.integerList.size() == 1;
		assert hc.integerList.get(0) == null;

		Entity e = ofy.getDatastore().get(fact.getRawKey(key));
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
		Objectify ofy = this.fact.begin();

		HasCollections hc = new HasCollections();
		hc.integerList = null;

		Key<HasCollections> key = ofy.put(hc);
		hc = ofy.get(key);

		ofy.put(hc);
		hc = ofy.get(key);
		assert hc.integerList == null;	// not loaded

		Entity e = ofy.getDatastore().get(fact.getRawKey(key));
		// rule : never store a null collection
		assert !e.hasProperty("integerList");
	}

	/**
	 * Test rule: never store an empty Collection, leaves value as null
	 */
	@Test
	public void testEmptyCollections() throws Exception
	{
		Objectify ofy = this.fact.begin();

		HasCollections hc = new HasCollections();
		hc.integerList = new ArrayList<Integer>();

		Key<HasCollections> key = ofy.put(hc);
		hc = ofy.get(key);

		System.out.println(ofy.getDatastore().get(fact.getRawKey(hc)));

		// This isn't valid with the caching objectify turned on
		assert hc.integerList == null;

		Entity e = ofy.getDatastore().get(fact.getRawKey(key));
		// rule : never store an empty collection
		assert !e.hasProperty("integerList");

		assert hc.initializedList != null;
		assert hc.initializedList instanceof LinkedList<?>;
	}

	/** */
	public static class HasInitializedCollection
	{
		public @Id Long id;
		public List<String> initialized = new ArrayList<String>();
		@Transient public List<String> copyOf;
		
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
		HasInitializedCollection fetched = this.putAndGet(has);
		assert fetched.initialized == fetched.copyOf;	// should be same object
		
		has = new HasInitializedCollection();
		has.initialized.add("blah");
		fetched = this.putAndGet(has);
		assert fetched.initialized == fetched.copyOf;	// should be same object
	}
	
	/**
	 * Without the generic type
	 */
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
		
		HasRawCollection fetched = this.putAndGet(hrc);
		
		assert hrc.raw.equals(fetched.raw);
	}
}
