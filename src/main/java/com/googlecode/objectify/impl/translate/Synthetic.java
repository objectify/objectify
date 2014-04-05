package com.googlecode.objectify.impl.translate;

/**
 * Marker interface for a translator that indicates that on load, the property value in the entity
 * should be ignored and instead the loader should just be run with a null value. This is a somewhat
 * experimental approach to handling @Owner fields.
 *
 * @author Jeff Schnitzer
 */
public interface Synthetic {
}
