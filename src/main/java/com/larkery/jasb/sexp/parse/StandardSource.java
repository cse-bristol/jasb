package com.larkery.jasb.sexp.parse;

import java.net.URI;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.larkery.jasb.sexp.ISExpression;
import com.larkery.jasb.sexp.ISExpressionSource;
import com.larkery.jasb.sexp.NodeBuilder;
import com.larkery.jasb.sexp.SExpressions;
import com.larkery.jasb.sexp.errors.IErrorHandler;
import com.larkery.jasb.sexp.errors.UnfinishedExpressionException;
import com.larkery.jasb.sexp.module.Module;
import com.larkery.jasb.sexp.parse.Includer.IResolver;
import com.larkery.jasb.sexp.template.Templates;

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
	
	@Override
	public ISExpression get(final URI address, final IErrorHandler errors) {
		ISExpression source = Includer.source(resolver, address, errors);
		
		final ImmutableList.Builder<IMacro> macros = ImmutableList.builder();
		
		final Module module = new Module();
		if (expandTemplates) {
			// rewrite modules
			
			source = MacroExpander.expand(ImmutableList.<IMacro>of(module), source, errors);
			
			final NodeBuilder output = NodeBuilder.create();
			
			final List<IMacro> templates = Templates.extract(source, output, errors);
			
			try {
				source = SExpressions.inOrder(output.getAll());
			} catch (final UnfinishedExpressionException e) {
				errors.handle(e.getError());
			}
			
			macros.addAll(templates);			
		}
		
		macros.add(module.getInitializerMacro());
		
		macros.addAll(extraMacros);
		
		final List<IMacro> macros_ = macros.build();
		if (macros_.isEmpty()) {
			return source;
		} else {
			return MacroExpander.expand(macros_, source, errors);
		}
	}

	public static ISExpressionSource createUntemplated(final IResolver resolver) {
		return new StandardSource(resolver, false, Collections.<IMacro>emptyList());
	}
}
