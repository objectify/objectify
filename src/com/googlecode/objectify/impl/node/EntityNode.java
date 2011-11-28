package com.googlecode.objectify.impl.node;

import com.googlecode.objectify.impl.Path;

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
abstract public class EntityNode
{
	/** Current path to this node */
	Path path;
	
	/** Only use this constructor during save() operations */
	public EntityNode(Path path) {
		this.path = path;
	}
	
	/**
	 * Gets the path to this node
	 */
	public Path getPath() {
		return this.path;
	}
}
