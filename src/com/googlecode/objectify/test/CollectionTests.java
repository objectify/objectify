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
import java.util.TreeSet;

import com.google.appengine.api.datastore.Entity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.test.entity.HasCollections;
import com.googlecode.objectify.test.entity.Trivial;
import com.googlecode.objectify.test.entity.HasCollections.CustomSet;

/**
 * Tests of various collection types
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class CollectionTests extends TestBase
{
	/** */
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(CollectionTests.class);

	/** */
	private void assertContains123(Collection<Integer> coll, Class<?> expectedClass)
	{
		assert coll.getClass() == expectedClass;

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
	public void testOKeySet() throws Exception
	{
		Objectify ofy = this.fact.begin();

		Key<Trivial> key7 = new Key<Trivial>(Trivial.class, 7);
		Key<Trivial> key8 = new Key<Trivial>(Trivial.class, 8);
		Key<Trivial> key9 = new Key<Trivial>(Trivial.class, 9);

		HasCollections hc = new HasCollections();
		hc.oKeySet = new HashSet<Key<Trivial>>();
		hc.oKeySet.add(key7);
		hc.oKeySet.add(key8);
		hc.oKeySet.add(key9);

		Key<HasCollections> key = ofy.put(hc);
		hc = ofy.get(key);

		assert hc.oKeySet instanceof HashSet<?>;
		assert hc.oKeySet.size() == 3;

		Iterator<Key<Trivial>> it = hc.oKeySet.iterator();
		assert it.next().equals(key7);
		assert it.next().equals(key8);
		assert it.next().equals(key9);
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
		List l = (List) e.getProperty("integerList");
		assert l != null;
		assert l.size() == 1;
		assert l.get(0) == null;
	}

	/**
	 * Rule: never store a null Collection, and always reconstruct an empty List.
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
		assert hc.integerList != null;
		assert hc.integerList.size() == 0;

		Entity e = ofy.getDatastore().get(fact.getRawKey(key));
		// rule : never store a null collection
		assert !e.hasProperty("integerList");
	}

	/**
	 * Test rule: never store an empty Collection, and always reconstruct an empty List.
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

		assert hc.integerList != null;
		assert hc.integerList.size() == 0;

		Entity e = ofy.getDatastore().get(fact.getRawKey(key));
		// rule : never store an empty collection
		assert !e.hasProperty("integerList");
	}
}
