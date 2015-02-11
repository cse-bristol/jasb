package com.larkery.jasb.sexp.errors;

import com.larkery.jasb.sexp.Location;
import com.larkery.jasb.sexp.Node;
import com.larkery.jasb.sexp.errors.IErrorHandler.IError;

public class BasicError implements IError {
	private final Location location;
	private final String message;
	private final Type type;
	
	public BasicError(final Location location, final String message,
					  final Type type) {
		super();
		this.location = location;
		this.message = message;
		this.type = type;
	}
	
	@Override
	public Location getLocation() {
		return location;
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
		return new BasicError(node.getLocation(), string, Type.ERROR);
	}

	public static IError nowhere(final String message) {
		return new BasicError(null, message, Type.ERROR);
	}

	public static IError at(final Location location, final String message) {
		return new BasicError(location, message, Type.ERROR);
	}
	@Override
	public String toString() {
		return location + " " + getMessage();
	}

	public static IError warningAt(final Location location, final String format) {
		return new BasicError(location, format, Type.WARNING);
	}
}
