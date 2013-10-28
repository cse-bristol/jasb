package com.larkery.jasb.bind.id;

import com.larkery.jasb.sexp.Atom;

public class UnresolvableIdentifierException extends RuntimeException {
	private static final long serialVersionUID = 1L;
	private final Atom atom;
	
	public UnresolvableIdentifierException(Atom atom) {
		super();
		this.atom = atom;
	}

	public Atom getAtom() {
		return atom;
	}
	
}
