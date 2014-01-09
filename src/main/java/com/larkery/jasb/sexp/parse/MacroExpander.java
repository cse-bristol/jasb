package com.larkery.jasb.sexp.parse;

import com.larkery.jasb.sexp.ISExpression;

import java.util.Map;
import java.util.List;
import com.larkery.jasb.sexp.errors.IErrorHandler;
import com.google.common.collect.ImmutableMap;
import com.larkery.jasb.sexp.ISExpressionVisitor;
import com.google.common.base.Optional;
import com.larkery.jasb.sexp.NodeBuilder;
import com.larkery.jasb.sexp.Node;
import com.larkery.jasb.sexp.Invocation;
import java.util.HashMap;
import java.util.ArrayList;
import com.larkery.jasb.sexp.errors.UnfinishedExpressionException;
import com.larkery.jasb.sexp.errors.BasicError;
import com.larkery.jasb.sexp.Seq;
import com.google.common.collect.Sets;

public class MacroExpander {
	private final Map<String, IMacro> macros;
	private final IErrorHandler errors;

	public static ISExpression expand(final List<IMacro> macros, final ISExpression input, final IErrorHandler errors) {
		return new MacroExpander(macros, errors).expand(input);
	}

	private MacroExpander(final List<IMacro> macros, final IErrorHandler errors) {
		this.errors = errors;

		final ImmutableMap.Builder<String, IMacro> b = ImmutableMap.builder();

		for (final IMacro m  : macros) {
			b.put(m.getName(), m);
		}

		this.macros = b.build();
	}

	public ISExpression expand(final ISExpression input) {
		return new ExpandedExpression(input);
	}

	class ExpandedExpression implements ISExpression {
		final ISExpression unexpanded;

		public ExpandedExpression(final ISExpression unexpanded) {
			this.unexpanded = unexpanded;
		}

		@Override
		public void accept(final ISExpressionVisitor visitor) {
			unexpanded.accept(new Cutout<NodeBuilder>(visitor) {
					protected Optional<NodeBuilder> cut(final String s) {
						if (macros.containsKey(s)) {
							return Optional.of(NodeBuilder.create());
						} else {
							return Optional.absent();
						}
					}

					protected void paste(final NodeBuilder nb) {
						final Node node = nb.getBestEffort();

						if (node instanceof Seq) {
							final Seq unexpanded = (Seq) node;
							
							if (unexpanded.isEmpty()) {
								throw new RuntimeException("This should never happen - if pasting part of a macro, it should at least have a macro name");
							} else {

								final Invocation inv = Invocation.of(unexpanded, errors);

								if (inv != null) {
									final IMacro macro = macros.get(inv.name);
									
									if (validateMacroParameters(inv, macro)) {
										macro.transform(inv, errors).accept(this);
									}
								}
							}
						} else {
							throw new RuntimeException("This should never happen - if pasting a macro, we should see a Seq");
						}
					}
				});
		}
	}

	private boolean validateMacroParameters(final Invocation inv, final IMacro macro) {
		boolean valid = true;

		for (final String s : Sets.difference(macro.getRequiredArgumentNames(), inv.arguments.keySet())) {
			errors.handle(BasicError.at(inv.node, inv.name + " requires named argument " + s));
			valid = false;
		}

		for (final String s : Sets.difference(inv.arguments.keySet(), macro.getAllowedArgumentNames())) {
			valid = false;
			errors.handle(BasicError.at(inv.arguments.get(s), inv.name + " does not expect argument " + s));
		}

		if (inv.remainder.size() < macro.getMinimumArgumentCount()) {
			errors.handle(BasicError.at(inv.node, 
										inv.name + " expects at least " + 
										macro.getMinimumArgumentCount() + " unnamed arguments"));
			valid = false;
		}

		if (inv.remainder.size() > macro.getMaximumArgumentCount()) {
			errors.handle(BasicError.at(inv.remainder.get(macro.getMaximumArgumentCount()), inv.name + " expects at most " + macro.getMinimumArgumentCount() + " unnamed arguments, but there are " + inv.remainder.size()));
			valid = false;
		}

		return valid;
	}
}
