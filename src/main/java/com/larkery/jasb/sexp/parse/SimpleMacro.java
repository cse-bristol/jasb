package com.larkery.jasb.sexp.parse;

import java.util.Set;

import com.google.common.collect.Sets;
import com.larkery.jasb.sexp.ISExpression;
import com.larkery.jasb.sexp.Invocation;
import com.larkery.jasb.sexp.Seq;
import com.larkery.jasb.sexp.errors.BasicError;
import com.larkery.jasb.sexp.errors.IErrorHandler;

public abstract class SimpleMacro implements IMacro {
	protected abstract Set<String> getRequiredArgumentNames();
	protected abstract Set<String> getAllowedArgumentNames();

	protected abstract int getMaximumArgumentCount();

	protected abstract int getMinimumArgumentCount();

	protected ISExpression expandResult(final IMacroExpander expander, final ISExpression transformed) {
		return expander.expand(transformed);
	}
	
	@Override
	public ISExpression transform(
			final Seq input,
			final IMacroExpander expander, 
			final IErrorHandler errors) {
		if (input instanceof Seq) {
			final Seq unexpanded = input;
			
			if (unexpanded.isEmpty()) {
				throw new RuntimeException("This should never happen - if pasting part of a macro, it should at least have a macro name");
			} else {

				final Invocation inv = Invocation.of(unexpanded, errors);

				if (inv != null) {
					if (validateMacroParameters(inv, errors)) {
						return expandResult(expander, doTransform(inv, expander, errors));
					}
				}
			}
		} else {
			throw new RuntimeException("This should never happen - if pasting a macro, we should see a Seq");
		}
		
		return ISExpression.EMPTY;
	}
	
	protected abstract ISExpression doTransform(final Invocation validated, final IMacroExpander expander, final IErrorHandler errors);
	
	protected boolean validateMacroParameters(final Invocation inv, final IErrorHandler errors) {
		boolean valid = true;

		for (final String s : Sets.difference(getRequiredArgumentNames(), inv.arguments.keySet())) {
			errors.handle(BasicError.at(inv.node, inv.name + " requires named argument " + s));
			valid = false;
		}

		for (final String s : Sets.difference(inv.arguments.keySet(), getAllowedArgumentNames())) {
			valid = false;
			errors.handle(BasicError.at(inv.arguments.get(s), inv.name + " does not expect argument " + s));
		}

		if (inv.remainder.size() < getMinimumArgumentCount()) {
			errors.handle(BasicError.at(inv.node, 
										inv.name + " expects at least " + 
										getMinimumArgumentCount() + " unnamed arguments"));
			valid = false;
		}

		if (inv.remainder.size() > getMaximumArgumentCount()) {
			errors.handle(BasicError.at(inv.remainder.get(getMaximumArgumentCount()), inv.name + " expects at most " + getMinimumArgumentCount() + " unnamed arguments, but there are " + inv.remainder.size()));
			valid = false;
		}

		return valid;
	}
}
