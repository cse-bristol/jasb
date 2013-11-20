package com.larkery.jasb.sexp;

import java.net.URI;

import com.larkery.jasb.sexp.errors.IErrorHandler;

public interface ISExpressionSource {
	public ISExpression get(final URI address, final IErrorHandler errors);
}
