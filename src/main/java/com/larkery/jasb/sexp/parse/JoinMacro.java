package com.larkery.jasb.sexp.parse;

import java.util.Collections;
import java.util.Set;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.larkery.jasb.sexp.Atom;
import com.larkery.jasb.sexp.Comment;
import com.larkery.jasb.sexp.ISExpression;
import com.larkery.jasb.sexp.Invocation;
import com.larkery.jasb.sexp.Node;
import com.larkery.jasb.sexp.errors.BasicError;
import com.larkery.jasb.sexp.errors.IErrorHandler;
import com.larkery.jasb.sexp.errors.UnfinishedExpressionException;

public class JoinMacro extends SimpleMacro {
	@Override
	public String getName() {
		return "join";
	}

	@Override
	public Set<String> getRequiredArgumentNames() {
		return Collections.emptySet();
	}

	@Override
	public Set<String> getAllowedArgumentNames() {
		return Collections.singleton("separator");
	}

	@Override
	public int getMaximumArgumentCount() {
		return Integer.MAX_VALUE;
	}

	@Override
	public int getMinimumArgumentCount() {
		return 1;
	}

	@Override
	public ISExpression doTransform(final Invocation expanded, final IMacroExpander expander, final IErrorHandler errors) {
		final ImmutableList.Builder<String> parts = ImmutableList.builder();

		try {
			for (final Node n : expanded.remainder) {
				if (n instanceof Comment) continue;
				
				
					final Node n2 = Node.copy(expander.expand(n));
					if (n2 instanceof Atom) {
						parts.add(((Atom)n2).getValue());
					} else {
						errors.handle(BasicError.at(n2, "concat can only join together atoms"));
					}
			}
	
			final String atomString;
			final Node node = Node.copy(expander.expand(expanded.arguments.get("separator")));
			if (node instanceof Atom) {
				atomString = Joiner.on(((Atom) node).getValue()).join(parts.build());			
			} else {
				atomString = Joiner.on("").join(parts.build());
				if (node != null) {
					errors.handle(BasicError.at(node, "the separator for concat should be an atom"));
				}
			}
			return Atom.create(atomString, expanded.node.getLocation());
		} catch (final UnfinishedExpressionException e) {
			errors.handle(e.getError());
		}
		
		return ISExpression.EMPTY;
	}
}
