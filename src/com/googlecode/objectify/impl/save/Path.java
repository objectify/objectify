package com.googlecode.objectify.impl.save;

/**
 * Path represents the individual steps from the root object to the current property.
 */
public class Path
{

	private static final Path ROOT = new Path("", null);

	/** This path segment. */
	private final String segment;
	/**
	 * The previous step in the path, null only for the special {@link #ROOT}
	 * element.
	 */
	private final Path previous;

	private Path(String name, Path path)
	{
		segment = name;
		previous = path;
	}

	public String toPathString()
	{
		StringBuilder builder = new StringBuilder();
		toPathString(builder);
		return builder.toString();
	}
	
	private void toPathString(StringBuilder builder) {
		// TODO cache generated path parts?
		if (previous != ROOT) {
			previous.toPathString(builder);
			builder.append('.');
		}
		builder.append(segment);
	}

	public Path extend(String name)
	{
		return new Path(name, this);
	}

	public static Path root()
	{
		return ROOT;
	}

	@Override
	public String toString()
	{
		return toPathString();
	}
}
