package com.larkery.jasb.sexp.errors;

import com.google.common.collect.ImmutableSet;
import com.larkery.jasb.sexp.Atom;
import com.larkery.jasb.sexp.Node;

public class UnresolvedReferenceError extends BasicError {
	private Atom reference;

	public UnresolvedReferenceError(
			final Atom reference) {
		super(ImmutableSet.of(reference.getLocation()), 
				ImmutableSet.<Node>of(reference), 
				String.format("could not find any element for reference " + reference.getValue()),
				Type.ERROR);
		this.reference = reference;
	}
	
	public Atom getReference() {
		return reference;
	}
}
