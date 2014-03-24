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
 * The context while creating translator factories. Tracks important state as we navigate the class graph.
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class CreateContext
{
	/** The objectify factory instance */
	ObjectifyFactory factory;
	public ObjectifyFactory getFactory() { return this.factory; }
	
	/** As we enter and exit embedded contexts, track the classes */
	Deque<Class<?>> owners = new ArrayDeque<Class<?>>(); 
	
	/** */
	public CreateContext(ObjectifyFactory fact) {
		this.factory = fact;
	}
	
//	/**
//	 * Call when entering a new class context.
//	 */
//	public void enterOwnerContext(Class<?> clazz) {
//		owners.addLast(clazz);
//	}
//
//	/**
//	 * Pops the class context; the parameter is a sanity check
//	 */
//	public void exitOwnerContext(Class<?> expected) {
//		Class<?> clazz = owners.removeLast();
//		assert clazz == expected;
//	}
//
//	/**
//	 * Search the owner chain for a compatible class; if nothing found, throw a user-friendly exception
//	 * @throws IllegalStateException if property class is not appropriate for the owner chain.
//	 */
//	public void verifyOwnerProperty(Path path, Property prop) {
//		Class<?> ownerClass = GenericTypeReflector.erase(prop.getType());
//
//		Iterator<Class<?>> ownersIt = owners.descendingIterator();
//		while (ownersIt.hasNext()) {
//			Class<?> potentialOwner = ownersIt.next();
//
//			if (ownerClass.isAssignableFrom(potentialOwner))
//				return;
//		}
//
//		throw new IllegalStateException("No compatible class matching " + prop + " in the owner hierarchy " + owners);
//	}
}