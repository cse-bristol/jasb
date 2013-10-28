package com.larkery.jasb.sexp.errors;

import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.larkery.jasb.sexp.Location;
import com.larkery.jasb.sexp.Node;
import com.larkery.jasb.sexp.errors.IErrorHandler.IError;
import com.larkery.jasb.sexp.errors.IErrorHandler.IError.Type;

public class BasicError implements IError {
	private final Set<Location> locations;
	private final Set<Node> nodes;
	private final String message;
	private final Type type;
	
	public BasicError(Set<Location> locations, Set<Node> nodes, String message,
			Type type) {
		super();
		this.locations = locations;
		this.nodes = nodes;
		this.message = message;
		this.type = type;
	}
	
	@Override
	public Set<Location> getLocations() {
		return locations;
	}

	@Override
	public Set<Node> getNodes() {
		return nodes;
	}

	@Override
	public String getMessage() {
		return message;
	}

	@Override
	public Type getType() {
		return type;
	}

	public static IError at(Node node, String string) {
		return new BasicError(ImmutableSet.of(node.getLocation()), ImmutableSet.of(node), string, Type.ERROR);
	}

	public static IError nowhere(String message) {
		return new BasicError(ImmutableSet.<Location>of(), ImmutableSet.<Node>of(), message, Type.ERROR);
	}

	public static IError at(Location location, String message) {
		return new BasicError(ImmutableSet.<Location>of(location), ImmutableSet.<Node>of(), message, Type.ERROR);
	}

}
