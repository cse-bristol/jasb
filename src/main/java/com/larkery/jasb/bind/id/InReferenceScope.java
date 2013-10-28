package com.larkery.jasb.bind.id;

/**
 * Indicates that a thing will only be unmapped after all the things which have 
 * {@link CreatesReferenceScope} on them with the same {@link #value()}.
 * 
 * This effectively sequences the scoping of all the identifiers in the current element.
 * 
 * @author hinton
 *
 */
public @interface InReferenceScope {
	String value();
}
