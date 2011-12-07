package com.googlecode.objectify.impl;


/**
 * This is a wrapper for a Path that makes it backwards; instead of going from tail to head,
 * it has links from head to tail.  This is sometimes useful for forward traversal of a path.
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class ForwardPath
{
	/** This path segment. */
	private final Path path;

	/** The next step in the path, null for the end. */
	private ForwardPath next;

	/** */
	public ForwardPath(Path path) {
		this.path = path;
	}

	/** 
	 * Recursive method which reverses the path into a ForwardPath.
	 * @param path cannot be the root path. 
	 */
	public static ForwardPath of(Path path) {
		ForwardPath next = new ForwardPath(path);
		if (path.getPrevious() == Path.root())
			return next;
		
		ForwardPath previous = of(path.getPrevious());
		previous.next = next;
		
		return previous;
	}
	
	/** @return the real full path to this place */
	public Path getPath() {
		return this.path;
	}
	
	/** @return the next path in the forward traversal */
	public ForwardPath getNext() {
		return this.next;
	}
	
	/** Get the complete path in this chain, typically for error messages or debugging */
	public Path getFinalPath() {
		ForwardPath here = this;
		while (here.next != null)
			here = here.next;
		
		return here.getPath();
	}
	
	@Override
	public String toString() {
		return path.toPathString();
	}
}
