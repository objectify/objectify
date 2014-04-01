package com.googlecode.objectify.impl.translate;

/**
 * Combines Translator with UsesExistingValue, useful so that we can create anonymous classes
 *
 * @author Jeff Schnitzer
 */
public interface TranslatorUsesExistingValue<P, D> extends Translator<P, D>, UsesExistingValue {
}
