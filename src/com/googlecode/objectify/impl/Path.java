package com.googlecode.objectify.impl;

/**
 * Path represents the individual steps from the root object to the current property.
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class Path
{
	/** */
	private static final Path ROOT = new Path("", null);
	public static Path root() {
		return ROOT;
	}

	/** This path segment. */
	private final String segment;

	/** The previous step in the path, null only for the special {@link #ROOT} element. */
	private final Path previous;

	/** */
	private Path(String name, Path path) {
		segment = name;
		previous = path;
	}

	/** Create the full x.y.z string */
	public String toPathString() {
		StringBuilder builder = new StringBuilder();
		toPathString(builder);
		return builder.toString();
	}
	
	/** */
	private void toPathString(StringBuilder builder) {
		if (previous != ROOT) {
			previous.toPathString(builder);
			builder.append('.');
		}
		
		builder.append(segment);
	}

	/** */
	public Path extend(String name) {
		return new Path(name, this);
	}
	
	/** Get this segment of the path.  For root this will be null. */
	public String getSegment() {
		return segment;
	}

	/** */
	@Override
	public String toString() {
		return toPathString();
	}

	/** Convenient way to include path location in the exception message.  Never returns. */
	public Object throwIllegalState(String message) {
		throw new IllegalStateException("Error at " + this + ": " + message);
	}

	/** Convenient way to include path location in the exception message.  Never returns. */
	public Object throwIllegalState(String message, Throwable cause) {
		throw new IllegalStateException("Error at " + this + ": " + message, cause);
	}
}
