package com.larkery.jasb.sexp.errors;

import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.larkery.jasb.sexp.Location;
import com.larkery.jasb.sexp.Node;

public class UnusedTermError extends BasicError {
	public UnusedTermError(final Set<Node> nodes) {
		super(locations(nodes), nodes, "Unexpected terms", Type.ERROR);
	}

	private static Set<Location> locations(Set<Node> nodes) {
		final ImmutableSet.Builder<Location> locations = ImmutableSet.builder();
		for (final Node node : nodes) {
			locations.add(node.getLocation());
		}
		return locations.build();
	}
}
