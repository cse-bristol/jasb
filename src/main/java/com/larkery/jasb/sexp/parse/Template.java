package com.larkery.jasb.sexp.parse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.larkery.jasb.sexp.Atom;
import com.larkery.jasb.sexp.Comment;
import com.larkery.jasb.sexp.Delim;
import com.larkery.jasb.sexp.INodeVisitor;
import com.larkery.jasb.sexp.ISExpression;
import com.larkery.jasb.sexp.ISExpressionVisitor;
import com.larkery.jasb.sexp.Invocation;
import com.larkery.jasb.sexp.Location;
import com.larkery.jasb.sexp.Node;
import com.larkery.jasb.sexp.Seq;
import com.larkery.jasb.sexp.errors.BasicError;
import com.larkery.jasb.sexp.errors.IErrorHandler;
import com.larkery.jasb.sexp.errors.IErrorHandler.IError;

/**
 * A template is a simple macro which substitutes a set of arguments into
 * a fixed structure
 */
public class Template extends SimpleMacro {
	private static final String TEMPLATE = "template";
	private static final String AT = "@";
	/* NOTHING is deliberately put in as the default for optional arguments which do not have an explicit default. */
	private static final Comment NOTHING = Comment.create("");

	private final String name;
	private final List<Node> body;
	
	private final Args[] all;
	private final NamedArgs named;
	private final NumberedArgs numbered;
	private final RestArgs rest;
	
	private Template(final String name,
					 final List<Node> body,
					 final NamedArgs named,
					 final NumberedArgs numbered,
					 final RestArgs rest,
					 final Args[] allArgs) {
		this.name = name;
		this.body = body;
		this.named = named;
		this.numbered = numbered;
		this.rest = rest;
		this.all = allArgs;
	}

	/**
	 * Strip templates out of the given input, showing the template-free but unexpanded output to the output visitor,
	 * and logging any errors to errors.
	 *
	 * Return a list of extracted macros, which can be used for substitution.
	 */
	public static List<IMacro> stripTemplates(final ISExpression input, 
											  final ISExpressionVisitor output, 
											  final IErrorHandler errors) {
		
		final Set<String> templateNames = new HashSet<String>();
		final ImmutableList.Builder<IMacro> result = ImmutableList.builder();

		input.accept(new Editor(output) {
			@Override
			protected Action act(final String name) {
				if (name.equals(TEMPLATE)) {
					return Action.SingleEdit;
				} else {
					return Action.Pass;
				}
			}
			@Override
			protected ISExpression edit(final Seq node) {
				final Optional<Template> t = Template.of(node, errors);
				 if (t.isPresent()) {
					 final Template template = t.get();
					 if (templateNames.contains(template.getName())) {
						 errors.handle(BasicError.at(node, "Duplicate definition of " + template.getName()));
					 } else {
						 templateNames.add(template.getName());
						 result.add(template);
					 }
				 }
				return ISExpression.EMPTY;
			}
		});

		return result.build();
	}

	/**
	 * First part of construction helper;
	 * checks the basic structure is correct
	 */
	static Optional<Template> of(final Node definition, final IErrorHandler errors) {
		if (definition instanceof Seq) {
			final Seq seq = (Seq) definition;

			final Optional<Node> head_ = seq.exceptComments(0);
			final Optional<Node> name_ = seq.exceptComments(1);
			final Optional<Node> args_ = seq.exceptComments(2);

			if (head_.isPresent() && name_.isPresent() && args_.isPresent()) {
				final Node head = head_.get();
				final Node name = name_.get();
				final Node args = args_.get();
				
				if (head instanceof Atom && name instanceof Atom && args instanceof Seq) {
					final String sHead = ((Atom) head).getValue();
					if (sHead.equals(TEMPLATE)) {
						return of(((Atom)name).getValue(),
								  (Seq) args,
								  seq.getNodesAfter(args),
								  errors);
					}
				}
			}

			errors.handle(BasicError.at(definition, "A template should have the form (template template-name [template-arguments] ...)"));
			return Optional.<Template>absent();
		} else {
			throw new IllegalArgumentException("A template ought not to be constructed except from a sequence (this is a programmer error). " + definition + " is not a sequence.");
		}
	}

	static boolean isLegalTemplateArgumentAtom(final Atom node) {
		return node.getValue().length() > 1 &&  node.getValue().startsWith(AT);
	}

	static boolean isLegalTemplateArgument(final Node node) {
		if (node instanceof Atom) {
			return isLegalTemplateArgumentAtom((Atom) node);
		} else if (node instanceof Seq) {
			final List<Node> parts = ((Seq) node).exceptComments();

			if (parts.size() == 1 || parts.size() == 2) {
				return isLegalTemplateArgumentAtom((Atom) parts.get(0));
			}
		}
		return false;
	}

	/**
	 * Second part of construction helper; checks that args represents some valid args.
	 */
	static Optional<Template> of(final String name, final Seq args, final List<Node> body, final IErrorHandler errors) {
		// separate args into bits.

		final NumberedArgs numbered = new NumberedArgs();
		final RestArgs rest = new RestArgs();
		final NamedArgs named = new NamedArgs();
		final Args[] argsHandlers = new Args[]{numbered, rest, named};
		
		for (final Node node : args.exceptComments()) {
			if (isLegalTemplateArgument(node)) {
				try {
				
					if (node instanceof Atom) {
						final String arg = ((Atom) node).getValue().substring(1);
						
						for (final Args a : argsHandlers) {
							if (a.maybeHandle(node, arg)) {
								break;
							}
						}
						
					} else if (node instanceof Seq) {
						final List<Node> parts = ((Seq) node).exceptComments();
						final String arg = ((Atom) parts.get(0)).getValue().substring(1);
	
						for (final Args a : argsHandlers) {
							if (a.maybeHandle(node, parts, arg)) {
								break;
							}
						}
					}
				
				} catch (final TemplateDefinitionException e) {
					errors.handle(e.error);
					return Optional.<Template>absent();
				}
			} else {
				errors.handle(BasicError.at(node, "Template arguments should be atoms like " + AT + "arg, or lists like ["+AT+ "arg default]"));
				return Optional.<Template>absent();
			}
		}
		
		// validate body
		final INodeVisitor argcheck = new INodeVisitor() {
				@Override
				public boolean seq(final Seq seq) {
					return true;
				}
				@Override
				public void atom(final Atom atom) {
					if (atom.getValue().startsWith(AT)) {
						final String arg = atom.getValue().substring(1);
						try {
							for (final Args a : argsHandlers) {
								if (a.check(atom, arg)) {
									return; /* This atom confirmed ok. */
								}
							}
						} catch (final TemplateDefinitionException e) {
							errors.handle(e.error);
						}
					}
				}
				@Override
				public void comment(final Comment comment) {
					
				}
			};

		for (final Node n : body) {
			n.accept(argcheck);
		}

		return Optional.of(new Template(name, body, named, numbered, rest, argsHandlers));
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Set<String> getRequiredArgumentNames() {
		return named.requiredNamed();
	}

	@Override
	public Set<String> getAllowedArgumentNames() {
		return named.allNamed();
	}

	@Override
	public int getMaximumArgumentCount() {
		/* Be careful of integer overflows. */
		if (rest.exists()) {
			return Integer.MAX_VALUE;
		} else {
			return numbered.allPositional();
		}
	}

	@Override
	public int getMinimumArgumentCount() {
		return numbered.requiredPositional();
	}

	@Override
	public ISExpression doTransform(final Invocation expanded, final IMacroExpander expander, final IErrorHandler errors) {
		final Map<String, List<Node>> arguments = new HashMap<String, List<Node>>();
		
		List<Node> remaining = expanded.remainder;
		for (final Args a : all) {
			final TransformResult r = a.doTransform(expanded, remaining);
			arguments.putAll(r.namedArgs);
			remaining = r.unhandled;
		}
		
		return new Substitution(expanded.node.getLocation(), body, arguments);
	}

	static class Substitution implements ISExpression {
		private final List<Node> body;
		private final Map<String, List<Node>> arguments;
		private final Location baseLocation;
		
		public Substitution(final Location baseLocation, final List<Node> body, final Map<String, List<Node>> arguments) {
			this.body = body;
			this.arguments = arguments;
			this.baseLocation = baseLocation.withTypeOfTail(Location.Type.Template);
		}

		@Override
		public void accept(final ISExpressionVisitor visitor) {
			final SubbingVisitor sv = new SubbingVisitor(visitor);
			for (final Node n : this.body) {
				n.accept(sv);
			}
		}

		class SubbingVisitor implements ISExpressionVisitor {
			private final ISExpressionVisitor delegate;
			private boolean rewritingLocation = true;
			
			public SubbingVisitor(final ISExpressionVisitor delegate) {
				this.delegate = delegate;
			}

			@Override
			public void locate(final Location loc) {
				// rewrite location to give location of error within template usage
				// so that errors associate to the place the template is used, not where
				// it is defined
				if (rewritingLocation) {
					delegate.locate(baseLocation.appending(loc));
				}
			}

			@Override
			public void open(final Delim delimeter) {
				delegate.open(delimeter);
			}

			@Override
			public void atom(final String string) {
				if (string.startsWith(AT) && arguments.containsKey(string.substring(1))) {
					// this is a template parameter for this template,
					// so we want to put that in for where we are; its
					// source location is al ready OK so we don't need
					// to rewrite it.
					final List<Node> nodes = arguments.get(string.substring(1));

					// disable location rewriting because we are visiting the argument and we want the error there
					rewritingLocation = false;
					for (final Node n : nodes) {
						if (n != NOTHING) {
							// strip out the dead comment
							n.accept(this);
						}
					}
					rewritingLocation = true;

				} else {
					delegate.atom(string);
				}
			}

			@Override
			public void comment(final String text) {
				delegate.comment(text);
			}

			@Override
			public void close(final Delim delimeter) {
				delegate.close(delimeter);
			}
		}
	}
	
	@SuppressWarnings("serial")
	static class TemplateDefinitionException extends Exception {

		private final IError error;

		public TemplateDefinitionException(final Node node, final String message) {
			this.error = BasicError.at(node, message);
		}
	}
	
	static interface Args {
		TransformResult doTransform(final Invocation expanded, final List<Node> remaining);
		/**
		 * @return true if this atom is confirmed ok, false if the other args should continue checking it 
		 */
		boolean check(final Atom atom, final String arg) throws TemplateDefinitionException;
		boolean maybeHandle(final Node node, final String arg) throws TemplateDefinitionException;
		boolean maybeHandle(final Node node, final List<Node> parts, final String arg) throws TemplateDefinitionException;
	}
	
	private static void putAll(final Map<String, List<Node>> namedArgs, final Map<String, Node> source) {
		for (final Entry<String, Node> e : source.entrySet()) {
			namedArgs.put(e.getKey(), ImmutableList.of(e.getValue()));
		}
	}
	
	static class NamedArgs implements Args {
		final HashSet<String> argsNoDefault = new HashSet<String>();
		final HashMap<String, Node> argsWithDefault = new HashMap<String, Node>();

		@Override
		public TransformResult doTransform(final Invocation expanded,
				final List<Node> remaining) {
			final HashMap<String, List<Node>> namedArgs = new HashMap<String, List<Node>>();
			
			putAll(namedArgs, argsWithDefault);
			putAll(namedArgs, expanded.arguments);

			return new TransformResult(namedArgs, remaining);
		}
		
		@Override
		public boolean maybeHandle(final Node node, final String arg) throws TemplateDefinitionException {
			if (argsNoDefault.contains(arg) || argsWithDefault.containsKey(arg)) {
				throw new TemplateDefinitionException(node, "Repeated template argument " + node);
			} else {
				argsNoDefault.add(arg);
				return true;
			}
		}

		@Override
		public boolean maybeHandle(final Node node, final List<Node> parts, final String arg) throws TemplateDefinitionException {
			if (argsNoDefault.contains(arg) || argsWithDefault.containsKey(arg)) {
				throw new TemplateDefinitionException(node, "Repeated template argument " + node);

			} else {
				if (parts.size() == 1) {
					argsWithDefault.put(arg, NOTHING);
				} else {
					argsWithDefault.put(arg, parts.get(1));
				}

				return true;
			}
		}

		public Set<String> requiredNamed() {
			return argsNoDefault;
		}

		public Set<String> allNamed() {
			return Sets.union(
					argsWithDefault.keySet(),
					argsNoDefault);
		}

		@Override
		public boolean check(final Atom atom, final String arg) throws TemplateDefinitionException {
			if (!(argsWithDefault.containsKey(arg) || argsNoDefault.contains(arg))) {
				throw new TemplateDefinitionException (atom, "Template body contains template variable " + atom + ", which is not in the template's argument list");
			} else {
				return true;
			}
		}
	}
	
	static class RestArgs implements Args {
		final List<Node> defaults = new ArrayList<>();
		private static final String REST = "rest";
		private static final Map<String, List<Node>> NO_NAMED_ARGS = ImmutableMap.<String, List<Node>>of();
		private static final List<Node> EMPTY = ImmutableList.of();
		boolean included = false;

		@Override
		public TransformResult doTransform(final Invocation expanded,
				final List<Node> remaining) {
			
			if (!included) {
				return new TransformResult(NO_NAMED_ARGS, remaining);
			} else if (remaining.isEmpty()) {
				return new TransformResult(ImmutableMap.of("rest", defaults), remaining);
			} else {
				return new TransformResult(ImmutableMap.of("rest", remaining), EMPTY);
			}
		}

		@Override
		public boolean maybeHandle(final Node node, final String arg) throws TemplateDefinitionException {
			if (arg.equals(REST)) {
				if (included) {
					throw new TemplateDefinitionException(node, "Repeated template argument " + node);
					
				} else {
					included = true;
					return true;
				}
			} else {
				return false;
			}
		}

		@Override
		public boolean maybeHandle(final Node node, final List<Node> parts, final String arg) throws TemplateDefinitionException {
			if (arg.equals(REST)) {
				if (included) {
					throw new TemplateDefinitionException(node, "Repeated template argument " + node);
					
				} else {
					included = true;
					defaults.addAll(parts.subList(1, parts.size()));
					return true;
					
				}
			} else {
				return false;
			}
		}

		public boolean exists() {
			return included;
		}

		@Override
		public boolean check(final Atom atom, final String arg)
				throws TemplateDefinitionException {
			if (arg.equals(REST)) {
				if (included) {
					return true;
				} else {
					throw new TemplateDefinitionException (atom, "Template body contains remainder template variable " + atom + ", which is not in the template's argument list");
				}
			} else {
				return false;
			}
		}
	}
	
	static class NumberedArgs implements Args {
		int count = 0;
		Map<String, Node> defaults = new HashMap<>();

		@Override
		public TransformResult doTransform(final Invocation expanded,
				final List<Node> remaining) {
			
			final Map<String, List<Node>> numbered = new HashMap<String, List<Node>>();
			putAll(numbered, defaults);
			
			for (int i = 0; i < count && i < remaining.size(); i++) {
				numbered.put(
						Integer.toString(i + 1), /* Arguments are 1-indexed, lists are 0-indexed. */ 
						ImmutableList.of(remaining.get(i)));
			}
			
			return new TransformResult(numbered, remaining.subList(
					Math.min(count, remaining.size()), remaining.size()));
		}

		@Override
		public boolean maybeHandle(final Node node, final String arg) throws TemplateDefinitionException {
			try {
				final int i = Integer.parseInt(arg);
				
				if (i == count + 1) {
					if (!defaults.isEmpty()) {
						throw new TemplateDefinitionException(node, "Mandatory numbered template argument should always be defined before all optional numbered arguments " + node);
					}
					
					count += 1;
					return true;
				} else {
					throw new TemplateDefinitionException(node, "Numbered template argument was out of order " + node);
				}
				
			} catch (final NumberFormatException e) {
				return false;
			}
		}

		@Override
		public boolean maybeHandle(final Node node, final List<Node> parts, final String arg) throws TemplateDefinitionException {
			try {
				final Integer i = Integer.parseInt(arg);
				
				if (i == count + 1) {
					count += 1;
					if (parts.size() == 1) {
						defaults.put(i.toString(), NOTHING);
					} else {
						defaults.put(i.toString(), parts.get(1));
					}
					
					return true;
					
				} else {
					throw new TemplateDefinitionException(node, "Template positional argument was out of order " + node);
				}
			} catch (final NumberFormatException e) {
				return false;
			}
		}

		public int requiredPositional() {
			return count - defaults.size();
		}

		public int allPositional() {
			return count;
		}

		@Override
		public boolean check(final Atom atom, final String arg)
				throws TemplateDefinitionException {
			try {
				final int i = Integer.parseInt(arg);
				
				if (i > count) {
					throw new TemplateDefinitionException (atom, "Template body contains numbered template variable " + atom + ", which is not in the template's argument list");
				} else {
					return true;
				}
				
			} catch (final NumberFormatException e) {
				return false;
			}
		}
	}
	
	static class TransformResult {
		private final List<Node> unhandled;
		private final Map<String, List<Node>> namedArgs;

		public TransformResult(final Map<String, List<Node>> namedArgs, final List<Node> unhandled) {
			this.namedArgs = namedArgs;
			this.unhandled = unhandled;
		}
	}
}
