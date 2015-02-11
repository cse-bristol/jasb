package com.larkery.jasb.sexp.errors;

import java.util.Set;

import com.google.common.collect.Iterables;
import com.larkery.jasb.sexp.Node;

public class UnusedTermError extends BasicError {
	public UnusedTermError(final Set<Node> nodes) {
		super(Iterables.get(nodes, 0).getLocation(), "Unexpected terms " + nodes, Type.ERROR);
	}
}
