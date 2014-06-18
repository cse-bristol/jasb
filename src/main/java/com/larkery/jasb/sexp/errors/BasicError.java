package com.larkery.jasb.sexp.errors;

import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.larkery.jasb.sexp.Location;
import com.larkery.jasb.sexp.Node;
import com.larkery.jasb.sexp.errors.IErrorHandler.IError;

public class BasicError implements IError {
	private final Set<Location> locations;
	private final Set<Node> nodes;
	private final String message;
	private final Type type;
	
	public BasicError(final Set<Location> locations, final Set<Node> nodes, final String message,
					  final Type type) {
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

	public static IError at(final Node node, final String string) {
		return new BasicError(
							  node.getLocation() == null ? ImmutableSet.<Location>of() :
							  ImmutableSet.of(node.getLocation()), ImmutableSet.of(node), string, Type.ERROR);
	}

	public static IError nowhere(final String message) {
		return new BasicError(ImmutableSet.<Location>of(), ImmutableSet.<Node>of(), message, Type.ERROR);
	}

	public static IError at(final Location location, final String message) {
		return new BasicError(ImmutableSet.<Location>of(location), ImmutableSet.<Node>of(), message, Type.ERROR);
	}
	
	public static IError atSeveral(final String message, final Location... locations) {
		return new BasicError(ImmutableSet.<Location>copyOf(locations), ImmutableSet.<Node>of(), message, Type.ERROR);
	}

	@Override
	public String toString() {
		return locations + " " + getMessage();
	}

	public static IError warningAt(final Location location, final String format) {
		return new BasicError(location == null ? ImmutableSet.<Location>of() : ImmutableSet.of(location), ImmutableSet.<Node>of(), format, Type.WARNING);
	}
}
