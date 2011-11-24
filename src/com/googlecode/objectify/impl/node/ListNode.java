package com.googlecode.objectify.impl.node;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.googlecode.objectify.impl.save.Path;

/**
 * ListNodes don't really have a path per-se, but they convey their path
 * to any MapNodes below.  Also note that since we can only ever have one
 * list structure in an Entity, ListNodes will always contain MapNodes.
 */
public class ListNode extends EntityNode implements Iterable<MapNode>
{
	/** */
	List<MapNode> list;
	
	/** */
	public ListNode(Path path) {
		super(path);
	}
	
	/** */
	public Iterator<MapNode> iterator() {
		return list().iterator();
	}
	
	/** */
	public MapNode add() {
		MapNode node = new MapNode(path);
		list().add(node);
		return node;
	}
	
	/** */
	private List<MapNode> list() {
		if (this.list == null)
			this.list = new ArrayList<MapNode>();
		
		return this.list;
	}
}
