package com.googlecode.objectify.impl.translate;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.google.common.collect.Sets;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.impl.Path;
import com.googlecode.objectify.impl.Property;
import com.googlecode.objectify.repackaged.gentyref.GenericTypeReflector;

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
	 * Track the # of times we enter an embed - this can be more than 1. 
	 */
	int embedDepth = 0;
	
	/** List of path points at which we start an embedded collection (including array) */
	Set<Path> embedCollectionPoints;
	
	/** Points which have type Object or EmbeddedEntity and therefore should not be munged into nodes */
	Set<Path> leaveEmbeddedEntityAlonePoints = Sets.newHashSet();
	
	/** As we enter and exit embedded contexts, track the classes */
	Deque<Class<?>> owners = new ArrayDeque<Class<?>>(); 
	
	/** */
	public CreateContext(ObjectifyFactory fact) {
		this.factory = fact;
	}
	
	/** */
	public void enterEmbed(Path path) {
		embedDepth++;
		addAlternateEmbedPath(path);
	}
	
	/**
	 * There are multiple paths that might lead to a place when dealing with embedded collections; this makes
	 * sure they all get into the embedCollectionPoints (if appropriate - they are only added if we are in
	 * an embedded collection. 
	 * @param alternate is another path to add as a potential embed collection point.  no effect if we are not in an embed collection.
	 */
	public void addAlternateEmbedPath(Path alternate) {
		if (isInCollection() && isInEmbed()) {
			if (embedCollectionPoints == null)
				embedCollectionPoints = new HashSet<Path>();
			
			embedCollectionPoints.add(alternate);
		}
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
	
	/**
	 * Call when entering a new class context.
	 */
	public void enterOwnerContext(Class<?> clazz) {
		owners.addLast(clazz);
	}
	
	/**
	 * Pops the class context; the parameter is a sanity check
	 */
	public void exitOwnerContext(Class<?> expected) {
		Class<?> clazz = owners.removeLast();
		assert clazz == expected;
	}
	
	/**
	 * Search the owner chain for a compatible class; if nothing found, throw a user-friendly exception
	 * @throws IllegalStateException if property class is not appropriate for the owner chain.
	 */
	public void verifyOwnerProperty(Path path, Property prop) {
		Class<?> ownerClass = GenericTypeReflector.erase(prop.getType());
		
		Iterator<Class<?>> ownersIt = owners.descendingIterator();
		while (ownersIt.hasNext()) {
			Class<?> potentialOwner = ownersIt.next();
			
			if (ownerClass.isAssignableFrom(potentialOwner))
				return;
		}
		
		throw new IllegalStateException("No compatible class matching " + prop + " in the owner hierarchy " + owners);
	}

	/** */
	public Set<Path> getLeaveEmbeddedEntityAlonePoints() {
		return leaveEmbeddedEntityAlonePoints;
	}
	
	/** */
	public void leaveEmbeddedEntityAloneHere(Path path) {
		leaveEmbeddedEntityAlonePoints.add(path);
	}
}