package com.googlecode.objectify.impl.translate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.impl.Path;

/** 
 * The context while creating translator factories. Tracks some important state as we navigate the class graph.
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class CreateContext
{
	/** The objectify factory instance */
	ObjectifyFactory factory;
	public ObjectifyFactory getFactory() { return this.factory; }
	
	/** Track the # of times we enter a collection - this should never be more than 1 */
	int collectionDepth;
	
	/** Track the # of times we enter an embed - this can be more than 1 */
	int embedDepth;
	
	/** List of path points at which we start an embedded collection (including array) */
	List<Path> embedCollectionPoints;
	
	/** */
	public CreateContext(ObjectifyFactory fact) {
		this.factory = fact;
	}
	
	/** */
	public void enterEmbed(Path path) {
		if (isInCollection()) {
			if (embedCollectionPoints == null)
				embedCollectionPoints = new ArrayList<Path>();
			
			embedCollectionPoints.add(path);
		}
		
		embedDepth++;
	}
	
	/** */
	public void exitEmbed() {
		assert isInEmbed();
		
		embedDepth--;
	}

	/** */
	public boolean isInEmbed() {
		return embedDepth > 0;
	}

	/** */
	public void enterCollection(Path path) {
		assert !isInCollection();
		collectionDepth++;
	}
	
	/** */
	public void exitCollection() {
		assert isInCollection();
		collectionDepth--;
	}
	
	/** */
	public boolean isInCollection() {
		return collectionDepth > 0;
	}

	/** */
	public List<Path> getEmbedCollectionPoints() {
		return (embedCollectionPoints == null) ? Collections.<Path>emptyList() : embedCollectionPoints;
	}
}