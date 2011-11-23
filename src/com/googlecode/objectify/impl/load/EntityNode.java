package com.googlecode.objectify.impl.load;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.googlecode.objectify.impl.LoadContext;
import com.googlecode.objectify.impl.save.Path;

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
public class EntityNode extends HashMap<String, EntityNode>
{
	private static final long serialVersionUID = 1L;
	
	/**
	 * This gets thrown by getPropertyValue(LoadContext) when we try to iterate past the end of a collection.    
	 */
	public static class DoneIteratingException extends RuntimeException {
		private static final long serialVersionUID = 1L;
		
		/** Override this to do nothing, speeds up performance by a lot and we don't need it */
		@Override
		public synchronized Throwable fillInStackTrace() { return this; }
	}
	
	/** Current path to this node */
	Path path;
	
	/** */
	Map<String, EntityNode> map;
	
	/** */
	List<EntityNode> list;
	
	/** If this node has a property value, it will be set here */
	Object propertyValue;
	
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
	
	
	
	/**
	 * Set the propertyValue value in the node.
	 */
	public void setPropertyValue(Object value) {
		this.propertyValue = value;
	}

	/** 
	 * Get the propertyValue value, if it exists. 
	 */
	public Object getPropertyValue() {
		return this.propertyValue;
	}
	
	/**
	 * Checks to see if we are iterating, and if so, fetches an individual entry out of what we
	 * should be able to safely assume is a List property.
	 * 
	 * @throws DoneIteratingException if the currentIndex exceeds the list size
	 */
	public Object getPropertyValue(LoadContext ctx) throws DoneIteratingException {
		if (ctx.getCurrentIndex() != null) {
			@SuppressWarnings("unchecked")
			List<Object> list = (List<Object>)getPropertyValue();
			
			if (ctx.getCurrentIndex() >= list.size())
				throw new DoneIteratingException();
			
			return list.get(ctx.getCurrentIndex());
		} else {
			return getPropertyValue();
		}
	}
}
