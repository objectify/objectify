/*
 * $Id: BeanMixin.java 1075 2009-05-07 06:41:19Z lhoriman $
 * $URL: https://subetha.googlecode.com/svn/branches/resin/rtest/src/org/subethamail/rtest/util/BeanMixin.java $
 */

package com.googlecode.objectify.test.entity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.persistence.Id;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Cached;

/**
 * An entity that has a variety of collection types.
 * Left off getters and setters for convenience.
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
@Cached
public class HasCollections
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
	
	public static class CustomSet extends HashSet<Integer>
	{
		private static final long serialVersionUID = 1L;
		public int tenTimesSize() { return this.size() * 10; }
	}
	
	public CustomSet customSet;
	
	/** This should give the system a workout */
	public Set<Key<Trivial>> typedKeySet;
}