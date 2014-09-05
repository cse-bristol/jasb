package com.larkery.jasb.sexp.module;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.larkery.jasb.sexp.Atom;
import com.larkery.jasb.sexp.Delim;
import com.larkery.jasb.sexp.ISExpression;
import com.larkery.jasb.sexp.Invocation;
import com.larkery.jasb.sexp.Node;
import com.larkery.jasb.sexp.SExpressions;
import com.larkery.jasb.sexp.Seq;
import com.larkery.jasb.sexp.errors.IErrorHandler;
import com.larkery.jasb.sexp.parse.IMacro;
import com.larkery.jasb.sexp.parse.IMacroExpander;
import com.larkery.jasb.sexp.parse.MacroExpander;
import com.larkery.jasb.sexp.parse.MacroModel;
import com.larkery.jasb.sexp.parse.SimpleMacro;

public class Module implements IMacro {
	public static ISExpression transform(final ISExpression input, final IErrorHandler errors) {
		final Module mod = new Module();
		return MacroExpander.expand(ImmutableList.<IMacro>of(mod), input, errors);
	}
	
	private static final String NAME = "~module";
	private static final String NOT_A_TEMPLATE = "apart from the name, every statement in a module must be a template definition";

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public ISExpression transform(final Seq input, final IMacroExpander expander, final IErrorHandler errors) {
		// module should have a name, and then a sequence of s-expressions which are the template definitions
		
		final List<Node> parts = input.exceptComments();
		if (parts.size() < 2 || !(parts.get(1) instanceof Atom)) {
			errors.error(
					parts.size() < 2 ? input : parts.get(1)
					, "a module must have a name as its first argument");
			return SExpressions.empty();
		}
		
		final String moduleName = ((Atom) parts.get(1)).getValue();
		
		final ImmutableList.Builder<Node> transformedBody = ImmutableList.builder(); 
		
		final ImmutableList.Builder<IMacro> templateNames = ImmutableList.builder(); 
		
		// next we process each thing inside the module
		for (final Node n : parts.subList(2, parts.size())) {
			if (n instanceof Seq) {
				final Seq s = (Seq) n;
				if (s.getDelimeter() != Delim.Paren) {
					errors.error(n, NOT_A_TEMPLATE);
					return SExpressions.empty();
				}
				final List<Node> children = s.exceptComments();
				
				if (children.isEmpty() || !(children.get(0) instanceof Atom) || !((Atom)children.get(0)).getValue().equals("template")) {
					errors.error(n, NOT_A_TEMPLATE);
					return SExpressions.empty();
				}
				
				if (children.size()<2 || !(children.get(1) instanceof Atom)) {
					errors.error(n, NOT_A_TEMPLATE);
					return SExpressions.empty();
				}
				
				if (children.size()<3 || !(children.get(2) instanceof Seq)) {
					errors.error(n, NOT_A_TEMPLATE);
					return SExpressions.empty();
				}
				
				final String name = ((Atom) children.get(1)).getValue();
				
				templateNames.add(new Renamer(name, moduleName + "/" + name));
			} else {
				errors.error(n, NOT_A_TEMPLATE);
				return SExpressions.empty();
			}
		}
		
		for (final Node n : parts.subList(2, parts.size())) {
			if (n instanceof Seq) {
				final Seq s = (Seq) n;
				final List<Node> children = s.exceptComments();
				final Atom name = (Atom) children.get(1);
				
				final Seq.Builder b = Seq.builder(s.getLocation(), s.getDelimeter());
				
				for (final Node c : s) {
					if (c == name) {
						b.add(Atom.create(moduleName + "/" + name.getValue(), name.getLocation()));
					} else {
						b.add(c);
					}
				}
				transformedBody.add(b.build(s.getEndLocation()));
			}
		}
		
		templateNames.add(new Variabler(moduleName));
		
		// we don't want to recursively expand modules within modules? or do we?
		
		return MacroExpander.expand(templateNames.build(), SExpressions.inOrder(transformedBody.build()), errors);
	}
	
	static class Variabler extends SimpleMacro {
		private final String moduleName;
		
		Variabler(final String moduleName) {
			super();
			this.moduleName = moduleName;
		}

		@Override
		public String getName() {
			return "~local";
		}

		@Override
		protected Set<String> getRequiredArgumentNames() {
			return Collections.emptySet();
		}

		@Override
		protected Set<String> getAllowedArgumentNames() {
			return Collections.emptySet();
		}

		@Override
		protected int getMaximumArgumentCount() {
			return Integer.MAX_VALUE;
		}

		@Override
		protected int getMinimumArgumentCount() {
			return 1;
		}

		@Override
		protected ISExpression doTransform(final Invocation validated, final IMacroExpander expander, final IErrorHandler errors) {
			final Seq.Builder out = Seq.builder(validated.node.getLocation(), Delim.Paren);
			out.add("~join");
			
			final Node first = validated.remainder.get(0);
			
			if (first instanceof Atom) {
				final Atom a = (Atom) first;
				if (a.getValue().startsWith("#")) {
					out.add(Atom.create("#" + moduleName + "/" + a.getValue().substring(1), a.getLocation()));
				} else {
					out.add(Atom.create(moduleName + "/" + a.getValue(), a.getLocation()));
				}
			}
			
			for (final Node n : validated.remainder.subList(1, validated.remainder.size())) {
				out.add(n);
			}
			
			return out.build(validated.node.getEndLocation());
		}
		
		@Override
		protected ISExpression expandResult(final IMacroExpander expander, final ISExpression transformed) {
			return transformed;
		}
		
		@Override
		public MacroModel getModel() {
			return MacroModel.builder()
					.desc("Creates module-specific names for variables, when used within ~module, by prefixing them with the modules name.")
					.pos().remainder("The values to use when constructing the module specific name").and()
					.build();
		}
	}
	
	static class Renamer implements IMacro {
		final String fromName;
		final String toName;
		
		public Renamer(final String fromName, final String toName) {
			super();
			this.fromName = fromName;
			this.toName = toName;
		}

		@Override
		public String getName() {
			return fromName;
		}

		@Override
		public ISExpression transform(final Seq input, final IMacroExpander expander, final IErrorHandler errors) {
			final Atom head = (Atom) input.exceptComments().get(0);
			final Seq.Builder b = Seq.builder(input.getLocation(), input.getDelimeter());
			
			for (final Node n : input) {
				if (n == head) {
					b.add(Atom.create(toName, n.getLocation()));
				} else {
					b.add(n);
				}
			}
			
			return expander.expand(b.build(input.getEndLocation()));
		}
		
		@Override
		public MacroModel getModel() {
			return MacroModel.builder()
					.desc("Renames uses of " + fromName + " to " + toName)
					.build();
		}
		
		@Override
		public Optional<Node> getDefiningNode() {
			return Optional.absent();
		}
	}
	
	@Override
	public MacroModel getModel() {
		return MacroModel.builder()
				.desc("A special macro, which rewrites any templates inside it to be prefixed with a name")
				.pos().require("A name for the module").and()
				.build();
	}
	
	@Override
	public Optional<Node> getDefiningNode() {
		return Optional.absent();
	}
}
