package com.larkery.jasb.sexp.parse;

import java.net.URI;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.larkery.jasb.sexp.ISExpression;
import com.larkery.jasb.sexp.ISExpressionSource;
import com.larkery.jasb.sexp.errors.IErrorHandler;
import com.larkery.jasb.sexp.module.Module;
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
	private final boolean expandTemplates;
	private final List<IMacro> extraMacros;
	
	StandardSource(final IResolver resolver, final boolean expandTemplates, final List<IMacro> extraMacros) {
		super();
		this.resolver = resolver;
		this.expandTemplates = expandTemplates;
		this.extraMacros = ImmutableList.copyOf(extraMacros);
	}

	public static final ISExpressionSource create(final IResolver resolver, final IMacro...extraMacros) {
		return new StandardSource(resolver, true, ImmutableList.copyOf(extraMacros));
	}
	
	private ISExpression withMacros(final ISExpression output, final IErrorHandler errors) {
		if (extraMacros.isEmpty()) return output;
		else return MacroExpander.expand(extraMacros, output, errors);
	}
	
	@Override
	public ISExpression get(final URI address, final IErrorHandler errors) {
		if (expandTemplates) {
			return withMacros(TemplateExpander.expand(
					Module.transform(
							Includer.source(resolver, address, errors), 
							errors), 
						errors), 
					errors);
		} else {
			return withMacros(Includer.source(resolver, address, errors), errors);
		}
	}

	public static ISExpressionSource createUntemplated(final IResolver resolver) {
		return new StandardSource(resolver, false, Collections.<IMacro>emptyList());
	}
}
