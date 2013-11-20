package com.larkery.jasb.sexp.parse;

import java.net.URI;

import com.larkery.jasb.sexp.ISExpression;
import com.larkery.jasb.sexp.ISExpressionSource;
import com.larkery.jasb.sexp.errors.IErrorHandler;
import com.larkery.jasb.sexp.parse.Includer.IResolver;

/**
 * Chains together parser, expander and includer
 * 
 * Includes first, then expands
 * 
 * @author hinton
 *
 */
public class StandardSource implements ISExpressionSource {
	private final IResolver resolver;
	
	StandardSource(final IResolver resolver) {
		super();
		this.resolver = resolver;
	}

	public static final ISExpressionSource create(final IResolver resolver) {
		return new StandardSource(resolver);
	}
	
	@Override
	public ISExpression get(final URI address, final IErrorHandler errors) {
		return /*Expander.expand(*/
				Includer.source(resolver, address, errors)
				/*, errors)*/;
	}
}
