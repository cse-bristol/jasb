package com.larkery.jasb.sexp.parse;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
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
import com.larkery.jasb.sexp.NodeBuilder;
import com.larkery.jasb.sexp.Seq;
import com.larkery.jasb.sexp.errors.BasicError;
import com.larkery.jasb.sexp.errors.IErrorHandler;

/**
 * A template is a simple macro which substitutes a set of arguments into
 * a fixed structure
 */
public class Template extends SimpleMacro {
	private static final String TEMPLATE = "template";
	private static final String AT = "@";
	private static final Comment NOTHING = Comment.create("");

	private final String name;
	private final List<Node> body;
	private final Map<String, Node> defaults;
	private final Set<String> noDefaults;
	private final Location baseLocation;

	private Template(final Location baseLocation,
					 final String name,
					 final List<Node> body,
					 final Map<String, Node> defaults,
					 final Set<String> noDefaults) {
		this.baseLocation = baseLocation;
		this.name = name;
		this.body = body;
		this.defaults = defaults;
		this.noDefaults = noDefaults;
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

		input.accept(new Cutout<NodeBuilder>(output) {
						 @Override
						protected Optional<NodeBuilder> cut(final String head) {
							 if (head.equals(TEMPLATE)) {
								 return Optional.of(NodeBuilder.create());
							 } else {
								 return Optional.<NodeBuilder>absent();
							 }
						 }

						 @Override
						protected void paste(final NodeBuilder b) {
							 final Node node = b.getBestEffort();
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
						return of(definition.getLocation(),
								  ((Atom)name).getValue(),
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
	static Optional<Template> of(final Location baseLocation, final String name, final Seq args, final List<Node> body, final IErrorHandler errors) {
		// separate args into bits.

		final HashSet<String> argsNoDefault = new HashSet<String>();
		final HashMap<String, Node> argsWithDefault = new HashMap<String, Node>();

		for (final Node node : args.exceptComments()) {
			if (isLegalTemplateArgument(node)) {
				if (node instanceof Atom) {
					final String arg = ((Atom) node).getValue().substring(1);
					if (argsNoDefault.contains(arg) || argsWithDefault.containsKey(arg)) {
						errors.handle(BasicError.at(node, "Repeated template argument " + node));
						return Optional.<Template>absent();
					} else {
						argsNoDefault.add(arg);
					}
				} else if (node instanceof Seq) {
					final List<Node> parts = ((Seq) node).exceptComments();
					final String arg = ((Atom) parts.get(0)).getValue().substring(1);
					if (argsNoDefault.contains(arg) || argsWithDefault.containsKey(arg)) {
						errors.handle(BasicError.at(node, "Repeated template argument " + node));
						return Optional.<Template>absent();
					} else {

						final Node defaultValue;
						if (parts.size() == 1) {
							defaultValue = NOTHING;
						} else {
							defaultValue = parts.get(1);
						}

						argsWithDefault.put(arg, defaultValue);
					}
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
						final String n = atom.getValue().substring(1);
						if (!(argsWithDefault.containsKey(n) || argsNoDefault.contains(n))) {
							errors.handle(BasicError.at(atom, "Template body contains template variable " + atom + ", which is not in the template's argument list"));
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

		return Optional.of(new Template(baseLocation, name, body, argsWithDefault, argsNoDefault));
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Set<String> getRequiredArgumentNames() {
		return noDefaults;
	}

	@Override
	public Set<String> getAllowedArgumentNames() {
		return Sets.union(noDefaults, defaults.keySet());
	}

	@Override
	public int getMaximumArgumentCount() {
		return 0;
	}

	@Override
	public int getMinimumArgumentCount() {
		return 0;
	}

	@Override
	public ISExpression doTransform(final Invocation expanded, final IMacroExpander expander, final IErrorHandler errors) {
		final Map<String, Node> arguments = new HashMap<String, Node>();
		
		arguments.putAll(defaults);
		arguments.putAll(expanded.arguments);

		return new Substitution(baseLocation, body, arguments);
	}

	static class Substitution implements ISExpression {
		private final List<Node> body;
		private final Map<String, Node> arguments;
		private final Location baseLocation;
		
		public Substitution(final Location baseLocation, final List<Node> body, final Map<String, Node> arguments) {
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
			
			public SubbingVisitor(final ISExpressionVisitor delegate) {
				this.delegate = delegate;
			}

			@Override
			public void locate(final Location loc) {
				// rewrite location to be inside template
				delegate.locate(baseLocation.appending(loc));
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
					// source location is already OK so we don't need
					// to rewrite it.
					final Node n = arguments.get(string.substring(1));

					if (n != NOTHING) {
						// strip out the dead comment
						n.accept(delegate);
					}
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
}
