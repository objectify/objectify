package com.googlecode.objectify.impl.node;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.googlecode.objectify.impl.Path;

/**
 * ListNodes don't really have a path per-se, but they convey their path
 * to any MapNodes below.  Also note that since we can only ever have one
 * list structure in an Entity, ListNodes will always contain MapNodes.
 */
public class ListNode extends EntityNode implements Iterable<EntityNode>
{
	/** */
	List<EntityNode> list;
	
	/** */
	public ListNode(Path path) {
		super(path);
	}
	
	/** */
	public int size() {
		return list().size();
	}
	
	/** */
	public Iterator<EntityNode> iterator() {
		return list().iterator();
	}
	
	/** */
	public MapNode add() {
		MapNode node = new MapNode(path);
		list().add(node);
		return node;
	}
	
	/** */
	public void add(EntityNode node) {
		list().add(node);
	}
	
	/**
	 * Gets the map node at the index, making sure that a map node exists there.  Assumes that the
	 * indexes are always fed in order.
	 */
	public MapNode pathMap(int index) {
		if (index == list().size())
			return add();
		else
			return (MapNode)list().get(index);
	}
	
	/** */
	private List<EntityNode> list() {
		if (this.list == null)
			this.list = new ArrayList<EntityNode>();
		
		return this.list;
	}
	
	/** */
	@Override
	public String toString() {
		return list().toString();
	}
	
	/** For testing */
	public EntityNode get(int index) {
		return list().get(index);
	}
}
