package com.larkery.jasb.sexp.parse;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Stack;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.larkery.jasb.sexp.Atom;
import com.larkery.jasb.sexp.Comment;
import com.larkery.jasb.sexp.Delim;
import com.larkery.jasb.sexp.ISExpression;
import com.larkery.jasb.sexp.ISExpressionVisitor;
import com.larkery.jasb.sexp.Invocation;
import com.larkery.jasb.sexp.Location;
import com.larkery.jasb.sexp.Node;
import com.larkery.jasb.sexp.NodeBuilder;
import com.larkery.jasb.sexp.Seq;
import com.larkery.jasb.sexp.errors.BasicError;
import com.larkery.jasb.sexp.errors.IErrorHandler;
import com.larkery.jasb.sexp.errors.UnfinishedExpressionException;

public class Expander {
	public static Node expand(final ISExpression source, final IErrorHandler errors) {
		final NodeBuilder withoutMacros = NodeBuilder.create();
		final Set<Node> macros = new HashSet<>();
		
		source.accept(new Cutout<NodeBuilder>(withoutMacros) {
			@Override
			protected Optional<NodeBuilder> cut(final String head) {
				if (head.equals("template")) {
					return Optional.of(NodeBuilder.create());
				} else {
					return Optional.absent();
				}
			}

			@Override
			protected void paste(final NodeBuilder q) {
				try {
					final Node node = q.get();
					macros.add(node);
				} catch (final UnfinishedExpressionException e) {
				}
			}
		});
		
		try {
			return substitute(macros, withoutMacros.getBestEffort(), errors);
		} catch (final NoSuchElementException | UnsupportedOperationException nse) {
			return Seq.builder(null, Delim.Paren).build(null);
		}
	}

	private static Node substitute(final Set<Node> macros, final Node node, final IErrorHandler errors) {
		boolean error = false;
		final Map<String, Template> templates = new HashMap<>();
		for (final Node n : macros) {
			final Template template = Template.from(n, errors);
			if (template == null) {
				error = true;
			} else {
				if (templates.containsKey(template.name)) {
					errors.handle(BasicError.at(n, "template " + template.name + " is defined twice"));
					error = true;
				} else {
					templates.put(template.name, template);
				}
			}
		}
		if (error) {
			errors.handle(BasicError.nowhere("Not expanding templates because of errors in macro definitions"));
			return node;
		} else {
			// process node
			final NodeBuilder output = NodeBuilder.create();
			
			final Stack<String> activeTemplates = new Stack<>();
			
			node.accept(new Cutout<NodeBuilder>(output) {
				@Override
				protected Optional<NodeBuilder> cut(final String head) {
					if (templates.containsKey(head)) {
						if (activeTemplates.contains(head)) {
							errors.handle(BasicError.at(templates.get(head).definingNode, "template expands recursively"));
							return Optional.absent();
						} else {
							activeTemplates.push(head);
							return Optional.of(NodeBuilder.create());
						}
					} else {
						return Optional.absent();
					}
				}

				@Override
				protected void paste(final NodeBuilder q) {
					final Node top = q.getBestEffort();
					
					if (top instanceof Seq) {
						final Seq seq = (Seq) top;
						final ISExpression expand = templates.get(((Atom)seq.getHead()).getValue()).expand(top, errors);
						if (expand != null) {
							expand.accept(this);
						}
					}
					
					activeTemplates.pop();
				}
			});
			
			return output.getBestEffort();
		}
	}
	
	static class Template {
		protected final Node definingNode;
		private final String name;
		private final ImmutableSet<String> parameters;
		private final ImmutableMap<String, Node> defaultParameters;
		private final ImmutableSet<Node> templateContents;

		public Template(final Node definition, final String name, final ImmutableSet<String> parameters, final ImmutableMap<String, Node> parameterDefaults, final ImmutableSet<Node> templateContents) {
			this.definingNode = definition;
			this.name = name;
			this.parameters = parameters;
			defaultParameters = parameterDefaults;
			this.templateContents = templateContents;
		}

		public ISExpression expand(final Node top, final IErrorHandler errors) {
			final Invocation invocation = Invocation.of(top, errors);
			if (invocation == null) return null;
			
			if (!invocation.remainder.isEmpty()) {
				errors.handle(BasicError.at(invocation.remainder.get(0), "Unexpected non-keyword template arguments"));
				return null;
			}
			
			boolean failed = false;
			final Map<String, Node> mapping = new HashMap<>();
			mapping.putAll(defaultParameters);
			for (final Map.Entry<String, Node> value : invocation.arguments.entrySet()) {
				final String withAt = "@"+value.getKey();
				if (!parameters.contains(withAt)) {
					errors.handle(BasicError.at(value.getValue(), "Unknown template argument " + value.getKey() + " for " + name));
					failed = true;
				} else {
					mapping.put(withAt, value.getValue());
				}
			}
			
			for (final String s : Sets.difference(mapping.keySet(), parameters)) {
				errors.handle(BasicError.at(top, "misssing template argument " + s));
				failed = true;
			}
			
			if (!failed) {
				return new ISExpression() {
					@Override
					public void accept(final ISExpressionVisitor visitor) {
						final ExpandingVisitor expander = new ExpandingVisitor(mapping, visitor);
						for (final Node node : templateContents) {
							node.accept(expander);
						}
					}
				};
			}
			
			return null;
		}
		
		static class ExpandingVisitor implements ISExpressionVisitor {
			final ISExpressionVisitor delegate;
			private final Map<String, Node> mapping;
			
			private ExpandingVisitor(final Map<String, Node> mapping, final ISExpressionVisitor delegate) {
				super();
				this.mapping = mapping;
				this.delegate = delegate;
			}

			@Override
			public void locate(final Location loc) {
				delegate.locate(loc);
			}

			@Override
			public void open(final Delim delimeter) {
				delegate.open(delimeter);
			}

			@Override
			public void atom(final String string) {
				if (mapping.containsKey(string)) {
					mapping.get(string).accept(delegate);
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

		public static Template from(final Node n, final IErrorHandler errors) {
			if (n instanceof Seq) {
				final Seq seq = (Seq) n;
				
				if (seq.size() < 3) {
					errors.handle(BasicError.at(seq, "templates must have a list of template arguments"));
					return null;
				}
				
				final List<Node> tail = seq.getTail();
				
				final ImmutableSet.Builder<String> templateParameters = ImmutableSet.builder();
				final ImmutableMap.Builder<String,Node> parameterDefaults = ImmutableMap.builder();
				
				final ImmutableSet.Builder<Node> templateContents = ImmutableSet.builder();
				
				final Node name = tail.get(0);
				final String nameValue;
				if (name instanceof Atom) {
					nameValue = ((Atom) name).getValue();
				} else {
					errors.handle(BasicError.at(seq, "the first part of a template definition should be the name"));
					return null;
				}
				
				// first bit of tail is macro parameters
				final Node parameters = tail.get(1);
				
				if (parameters instanceof Seq) {
					final Seq pseq = (Seq) parameters;
					String lastParameter = null;
					for (final Node parameter : pseq) {
						if (parameter instanceof Comment) {
							continue;
						}
						if (parameter instanceof Atom) {
							final Atom atom = (Atom) parameter;
							if (atom.getValue().startsWith("@")) {
								lastParameter = atom.getValue();
								templateParameters.add(lastParameter);
							} else {
								if (lastParameter == null) {
									errors.handle(BasicError.at(atom, "template parameters should start with @"));
									return null;
								} else {
									parameterDefaults.put(lastParameter, atom);
									lastParameter = null;
								}
							}
						} else {
							if (lastParameter == null) {
								errors.handle(BasicError.at(parameter, "template parameters should be single words"));
								return null;
							} else {
								parameterDefaults.put(lastParameter, parameter);
								lastParameter = null;
							}
						}
					}
					final ParameterChecker pc = new ParameterChecker(templateParameters.build(), errors);
					for (int i = 2; i<tail.size(); i++) {
						final Node node = tail.get(i);
						node.accept(pc);
						templateContents.add(node);
					}
					if (pc.errors) {
						return null;
					}
					
					return new Template(n, nameValue, templateParameters.build(), parameterDefaults.build(), templateContents.build());
				} else {
					errors.handle(BasicError.at(parameters, "template parameters should be a list"));
					return null;
				}
			} else {
				errors.handle(BasicError.at(n, "Template definition is not as expected"));
				return null;
			}
		}
	}
	
	static class ParameterChecker implements ISExpressionVisitor {
		private Location location;
		public boolean errors = false;
		private final Set<String> parameters;
		private final IErrorHandler handler;
		
		private ParameterChecker(final Set<String> parameters, final IErrorHandler handler) {
			super();
			this.parameters = parameters;
			this.handler = handler;
		}
		
		@Override
		public void locate(final Location loc) {
			this.location = loc;
		}

		@Override
		public void open(final Delim delimeter) {
		}

		@Override
		public void atom(final String string) {
			if (string.startsWith("@") && parameters.contains(string)==false) {
				errors = true;
				handler.handle(BasicError.at(location, string + " is not a parameter defined in this template"));
			}
		}

		@Override
		public void comment(final String text) {}
		
		@Override
		public void close(final Delim delimeter) {
		}
		
	}
}
