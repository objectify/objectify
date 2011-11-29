package com.googlecode.objectify.impl.translate;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

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
	
	/**
	 * Track the # of times we enter an embed - this can be more than 1.  This is also a little
	 * special because the Root class is handled by an instance of EmbedTranslatorFactory, so
	 * the first thing it does is enterEmbed() even though it really isn't an embedded.  This
	 * is a little bit of a hack, but the solution is to initialize the depth to -1. 
	 */
	int embedDepth = -1;
	
	/** List of path points at which we start an embedded collection (including array) */
	Set<Path> embedCollectionPoints;
	
	/** */
	public CreateContext(ObjectifyFactory fact) {
		this.factory = fact;
	}
	
	/** */
	public void enterEmbed(Path path) {
		if (isInCollection()) {
			if (embedCollectionPoints == null)
				embedCollectionPoints = new HashSet<Path>();
			
			embedCollectionPoints.add(path);
		}
		
		embedDepth++;
	}
	
	/**
	 * There are multiple paths that might lead to a place when dealing with embedded collections; this makes
	 * sure they all get into the embedCollectionPoints (if appropriate - they are only added if we are in
	 * an embedded collection. 
	 * @param alternate is another path to add as a potential embed collection point.  no effect if we are not in an embed collection.
	 */
	public void addAlternateEmbedPath(Path alternate) {
		if (isInCollection() && isInEmbed())
			embedCollectionPoints.add(alternate);
	}
	
	/** */
	public void exitEmbed() {
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
	public Set<Path> getEmbedCollectionPoints() {
		return (embedCollectionPoints == null) ? Collections.<Path>emptySet() : embedCollectionPoints;
	}
}