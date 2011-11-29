package com.googlecode.objectify.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


/**
 * <p>A tree repesentation of a datastore Entity object.  Note that actual propertyValue property values
 * are stored in an EntityNode under a special name; this is because there can be real properties
 * at higher levels of the tree.  For example:</p>
 * 
 * <ul>
 * <li>field1.field2 = "foo"</li>
 * <li>field1.field2.field3 = "bar"</li>
 * </ul>
 * 
 * <p>This produces a tree that looks like:</li>
 * 
 * <ul>
 * <li>
 * 	EntityNode (root)
 * 		<ul>
 * 			<li>
 * 				"field1": EntityNode
 * 				<ul>
 * 					<li>
 * 						"field2": EntityNode
 * 						<ul>
 * 							<li>propertyValue: "foo"</li>
 * 							<li>
 * 								"field3": EntityNode
 * 								<ul>
 * 									<li>propertyValue: "bar"</li>
 * 								</ul>
 * 							</li>
 * 						</ul>
 * 					</li>
 * 				</ul>
 * 			</li>
 * 		</ul>
 * </li>
 * <ul>
 */
public class EntityNode implements Iterable<EntityNode>
{
	/** Current path to this node */
	Path path;
	
	/** If this is a map node, this will have a value */
	Map<String, EntityNode> lazyMap;
	
	/** If this is a list node, this will have a value */
	List<EntityNode> lazyList;
	
	/** Because null is a legitimate value, we need to know if there is really a property value here */
	boolean hasProp;
	public boolean hasPropertyValue() { return this.hasProp; }
	
	/**
	 * A property value could appear at any point in the entity tree.  This is because we might attach
	 * denormalized data for the value at some point, and that data will likely be in a map underneath.
	 */
	Object propertyValue;
	public Object getPropertyValue() { return this.propertyValue; }
	
	/** */
	public void setPropertyValue(Object value) {
		this.propertyValue = value;
		this.hasProp = true;
	}

	/** */
	public void setPropertyValue(Object value, boolean index) {
		setPropertyValue(value);
		setPropertyIndexed(index);
	}
	
	/**
	 * Whether or not the property should be indexed on save.  During the load process this is ignored.
	 */
	boolean propertyIndexed;
	public boolean isPropertyIndexed() { return propertyIndexed; }
	public void setPropertyIndexed(boolean value) { propertyIndexed = value; }
	
	/** */
	public EntityNode(Path path) {
		this.path = path;
	}
	
	/**
	 * Gets the path to this node
	 */
	public Path getPath() {
		return this.path;
	}
	
	/** @return true if this node has a list with contents */
	public boolean hasList() {
		return this.lazyList != null;
	}

	/** @return true if this node has a map with contents */
	public boolean hasMap() {
		return this.lazyMap != null;
	}

	/** @return the size of the list or map, whichever is appropriate */
	public int size() {
		if (this.lazyMap != null)
			return this.lazyMap.size();
		else if (this.lazyList != null)
			return this.lazyList.size();
		else
			return 0;
	}
	
	/** Test for empty of any node type */
	public boolean isEmpty() {
		return size() == 0;
	}
	
	/** Iterate over the values in either the map or list, whichever is appropriate */
	public Iterator<EntityNode> iterator() {
		if (this.lazyMap != null)
			return this.lazyMap.values().iterator();
		else if (this.lazyList != null)
			return this.lazyList.iterator();
		else
			return Collections.<EntityNode>emptyList().iterator();
	}
	
	/** Creates a new node at same path, adds it to the list, and returns it */
	public EntityNode addToList() {
		EntityNode node = new EntityNode(path);
		list().add(node);
		return node;
	}
	
	/** Adds a node to the list */
	public void addToList(EntityNode node) {
		list().add(node);
	}
	
	/** Adds a node to the map, keyed by its path segment */
	public void addToMap(EntityNode node) {
		map().put(node.getPath().getSegment(), node);
	}
	
	/**
	 * Gets the node at the index of the list, making sure that a node exists there.  Assumes that the
	 * indexes are always fed in order.
	 */
	public EntityNode path(int index) {
		if (index == list().size())
			return addToList();
		else
			return list().get(index);
	}
	
	/**
	 * Gets the node with the specified key in the map, making sure that a node exists there. 
	 */
	public EntityNode path(String key) {
		EntityNode node = map().get(key);
		if (node == null) {
			node = new EntityNode(path.extend(key));
			map().put(key, node);
		}
		
		return node;
	}

	/** 
	 * Gets the node at the index of the list, assuming this is a list node
	 * @return null if nothing at that index
	 */
	public EntityNode get(int index) {
		return list().get(index);
	}
	
	/** Gets key on a map node */
	public EntityNode get(String key) {
		return map().get(key);
	}
	
	/** Puts a value on a map node */
	public void put(String key, EntityNode value) {
		map().put(key, value);
	}
	
	/** Checks to see of the map contains the key */
	public boolean containsKey(String key) {
		return map().containsKey(key);
	}
	
	/** Removes from map node */
	public EntityNode remove(String key) {
		return map().remove(key);
	}
	
	/** */
	private List<EntityNode> list() {
		if (this.lazyList == null) {
			assert lazyMap == null;
			this.lazyList = new ArrayList<EntityNode>();
		}
		
		return this.lazyList;
	}

	/** */
	private Map<String, EntityNode> map() {
		if (lazyMap == null) {
			assert lazyList == null;
			lazyMap = new HashMap<String, EntityNode>();
		}
		
		return lazyMap;
	}
	
	/** */
	@Override
	public String toString() {
		StringBuilder bld = new StringBuilder();
		
		if (hasPropertyValue())
			bld.append('\'').append(getPropertyValue()).append('\'');
		
		if (hasPropertyValue() && !isEmpty())
			bld.append('+');
		
		if (!isEmpty())
			if (lazyMap != null)
				bld.append(lazyMap.toString());
			else
				bld.append(lazyList.toString());
		
		return bld.toString();
	}
	
}
