package com.googlecode.objectify.impl;

/**
 * Functional interface for creating a Transactor. Allows us to have a bidirectional relationship with final fields.
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
@FunctionalInterface
public interface TransactorSupplier {
	Transactor get(final ObjectifyImpl ofy);
}