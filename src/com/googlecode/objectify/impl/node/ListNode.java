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
	
	/** */
	private List<EntityNode> list() {
		if (this.list == null)
			this.list = new ArrayList<EntityNode>();
		
		return this.list;
	}
}
