package com.larkery.jasb.sexp.parse;

import java.io.Reader;
import java.net.URI;

import com.larkery.jasb.sexp.ISExpression;
import com.larkery.jasb.sexp.ISExpressionVisitor;
import com.larkery.jasb.sexp.Location;
import com.larkery.jasb.sexp.errors.IErrorHandler;
import com.larkery.jasb.sexp.parse2.PolishParser;

public class Parser {
	public static ISExpression source(final URI location, final Reader reader, final IErrorHandler errors) {
		return new ISExpression() {
			@Override
			public void accept(final ISExpressionVisitor visitor) {
				final PolishParser pp = new PolishParser(location, reader);
				pp.parse(visitor);
			}
		};
	}

	public static ISExpression source(final Location location, final URI location2, final Reader reader, final IErrorHandler errors) {
		return new ISExpression() {
			@Override
			public void accept(final ISExpressionVisitor visitor) {
				final PolishParser pp = new PolishParser(location, location2, reader);
				pp.parse(visitor);
			}
		};
	}

}
