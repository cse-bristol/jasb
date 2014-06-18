package com.larkery.jasb.sexp.errors;

import com.larkery.jasb.sexp.Location;
import com.larkery.jasb.sexp.Node;

public abstract class BaseErrorHandler implements IErrorHandler {
	@Override
	public void handle(final Location location, final String format, final Object... interpolate) {
		handle(BasicError.at(location, String.format(format, interpolate)));
	}

	@Override
	public void handle(final Node location, final String format, final Object... interpolate) {
		handle(BasicError.at(location, String.format(format, interpolate)));
	}

}
