package com.googlecode.objectify.impl;

import java.util.Objects;

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
		if (this == ROOT) {
			return "";
		} else {
			StringBuilder builder = new StringBuilder();
			toPathString(builder);
			return builder.toString();
		}
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

	/** Get the previous path; for root this will be null */
	public Path getPrevious() {
		return previous;
	}

	/** */
	public boolean isRoot() { return this == ROOT; }

	/** */
	@Override
	public String toString() {
		return toPathString();
	}

	/** Compares on complete path */
	@Override
	public boolean equals(Object obj) {
		if (obj == null || obj.getClass() != this.getClass())
			return false;

		Path other = (Path)obj;

		if (!this.segment.equals(other.segment))
			return false;
		else
			return Objects.equals(this.previous, other.previous);
	}

	/** Generates hash code for complete path */
	@Override
	public int hashCode() {
		int hash = segment.hashCode();

		if (previous == null)
			return hash;
		else
			return hash ^ previous.hashCode();
	}

	/** Convenient way to include path location in the exception message.  Never returns. */
	public Object throwIllegalState(String message) {
		throw new IllegalStateException("At path '" + this + "': " + message);
	}

	/** Convenient way to include path location in the exception message.  Never returns. */
	public Object throwIllegalState(String message, Throwable cause) {
		throw new IllegalStateException("At path '" + this + "': " + message, cause);
	}

	/** Convenient way to include path location in the exception message.  Never returns. */
	public Object throwNullPointer(String message) {
		throw new NullPointerException("At path '" + this + "': " + message);
	}

	/**
	 * ROOT is 0, top level Entity properties are 1, embedded things are higher.
	 */
	public int depth() {
		int depth = 0;
		
		Path here = this;
		while (here != ROOT) {
			depth++;
			here = here.previous;
		}
		
		return depth;
	}
	
	/** Anything with a depth greater than 1 is embedded */
	boolean isEmbedded() {
		return depth() > 1;
	}
}
