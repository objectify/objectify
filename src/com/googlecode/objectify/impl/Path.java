package com.googlecode.objectify.impl;

import com.googlecode.objectify.util.LangUtils;

/**
 * Path represents the individual steps from the root object to the current property.
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class Path
{
	/**
	 * Unfortunately, we made a mistake in the original definition of the ^null property for embedded
	 * collections.  Instead of thing.^null we defined it as thing^null, which requires special casing.
	 * So we are fixing this now - we read thing^null as thing.^null, and always save as thing.^null.
	 * Some day in the far future we can remove this hack. 
	 */
	public static final String NULL_INDEXES = "^null";
	
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
	
	/** Convert an x.y.z path string back into a Path */
	public static Path of(String pathString) {
		if (pathString.length() == 0)
			return ROOT;
		else
			return ofImpl(ROOT, pathString, 0);
	}
	
	/** Recursive implementation of of() */
	private static Path ofImpl(Path here, String pathString, int begin) {
		int end = pathString.indexOf('.', begin);
		if (end < 0) {
			String part = pathString.substring(begin);
			
			// HACK HERE, see javadoc for NULL_INDEX
			if (part.length() > NULL_INDEXES.length() && part.endsWith(NULL_INDEXES)) {
				String base = part.substring(0, part.length()-NULL_INDEXES.length());
				return here.extend(base).extend(NULL_INDEXES);
			} else {
				return here.extend(part);
			}
		} else {
			String part = pathString.substring(begin, end);
			return ofImpl(here.extend(part), pathString, end+1);
		}
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
			return LangUtils.objectsEqual(this.previous, other.previous);
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
		throw new IllegalStateException(this + ": " + message);
	}

	/** Convenient way to include path location in the exception message.  Never returns. */
	public Object throwIllegalState(String message, Throwable cause) {
		throw new IllegalStateException(this + ": " + message, cause);
	}
}
