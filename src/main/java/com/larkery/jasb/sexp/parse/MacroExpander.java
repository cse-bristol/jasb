package com.larkery.jasb.sexp.parse;

import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.larkery.jasb.sexp.Atom;
import com.larkery.jasb.sexp.ISExpression;
import com.larkery.jasb.sexp.ISExpressionVisitor;
import com.larkery.jasb.sexp.Node;
import com.larkery.jasb.sexp.Seq;
import com.larkery.jasb.sexp.errors.BasicError;
import com.larkery.jasb.sexp.errors.IErrorHandler;

public class MacroExpander implements IMacroExpander {
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

	@Override
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
			unexpanded.accept(new Editor(visitor) {
				@Override
				protected Action act(final String name) {
					if (macros.containsKey(name)) {
						return Action.SingleEdit;
					} else {
						return Action.Pass;
					}
				}
				@Override
				protected ISExpression edit(final Seq unexpanded) {
					if (unexpanded.isEmpty()) {
						throw new RuntimeException("This should never happen - if pasting part of a macro, it should at least have a macro name");
					} else {
						final Node first = unexpanded.get(0);
						if (first instanceof Atom) {
							final String s = ((Atom) first).getValue();
							
							final IMacro macro = macros.get(s);
							
							try {
								return macro.transform(unexpanded, MacroExpander.this,  errors);
							} catch (final StackOverflowError soe) {
								errors.handle(BasicError.at(first, "Maximum macro expansion depth reached within " + s));
							}
						}
					}
					
					return ISExpression.EMPTY;
				}
			});
		}
	}
}
