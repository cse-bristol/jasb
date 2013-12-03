package com.larkery.jasb.sexp.errors;

import com.larkery.jasb.sexp.Node;

public class UnfinishedExpressionException extends Exception {
	private static final long serialVersionUID = 1L;
	private final Node mangled;

	public UnfinishedExpressionException(final Node mangled) {
		this.mangled = mangled;
	}
	
	public Node getBestEffort() {
		return mangled;
	}
}
