package com.googlecode.objectify.impl.translate;

/**
 * Marker interface for a translator that indicates that, when loading, it is sensitive to the previously
 * existing value in a field. This existing value is extracted and put in the LoadContext.
 *
 * @author Jeff Schnitzer
 */
public interface Recycles {
}
