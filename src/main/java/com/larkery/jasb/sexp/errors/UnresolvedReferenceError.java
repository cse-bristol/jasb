package com.larkery.jasb.sexp.errors;

import com.larkery.jasb.sexp.Atom;

public class UnresolvedReferenceError extends BasicError {
	private Atom reference;

	public UnresolvedReferenceError(final Atom reference) {
		super(reference.getLocation(), String
				.format("could not find any element for reference "
						+ reference.getValue()), Type.ERROR);
		this.reference = reference;
	}
	
	public Atom getReference() {
		return reference;
	}
}
