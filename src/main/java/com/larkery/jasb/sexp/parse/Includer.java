package com.larkery.jasb.sexp.parse;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.net.URI;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Stack;

import org.apache.commons.io.IOUtils;

import com.google.common.collect.ImmutableMap;
import com.larkery.jasb.sexp.Atom;
import com.larkery.jasb.sexp.Comment;
import com.larkery.jasb.sexp.INodeVisitor;
import com.larkery.jasb.sexp.ISExpression;
import com.larkery.jasb.sexp.ISExpressionVisitor;
import com.larkery.jasb.sexp.Location;
import com.larkery.jasb.sexp.Node;
import com.larkery.jasb.sexp.SExpressions;
import com.larkery.jasb.sexp.Seq;
import com.larkery.jasb.sexp.errors.BasicError;
import com.larkery.jasb.sexp.errors.IErrorHandler;
import com.larkery.jasb.sexp.errors.ResolutionException;
import com.larkery.jasb.sexp.errors.UnfinishedExpressionException;

public class Includer {
	public interface ILocationReader {
		public Reader getReader();
		public URI getLocation();
	}
	
	public static ILocationReader fileLocationReader(final URI ref) {
		return new ILocationReader(){

			@Override
			public Reader getReader() {
				try {
					return new InputStreamReader(ref.toURL().openStream());
				} catch (final IOException e) {
					throw new NoSuchElementException(e.getMessage());
				}
			}

			@Override
			public URI getLocation() {
				return ref;
			}
		};
	}
	
	/**
	 * A convenience method for making an {@link ILocationReader} which just reads from a string.
	 * @param location
	 * @param value
	 * @return
	 */
	public static ILocationReader stringLocationReader(final URI location, final String value) {
		return new ILocationReader(){
			@Override
			public Reader getReader() {
				return new StringReader(value);
			}
	
			@Override
			public URI getLocation() {
				return location;
			}
		};
	}
	
	public interface IResolver {
		/**
		 * This will be called with an expression starting with include, so for example
		 * 
		 * (include name: my-scenario version: 1)
		 * 
		 * Might become
		 * 
		 * nhm://name/my-scenario#1
		 * 
		 * @param include
		 * @param errors
		 * @return
		 * @throws ResolutionException 
		 */
		public URI convert(final Seq include, final IErrorHandler errors) throws ResolutionException;
		
		/**
		 * Get the content pointed to by a URI produced by {@link #convert(Seq, IErrorHandler)}
		 * 
		 * @param href
		 * @param errors
		 * @return
		 * @throws NoSuchElementException
		 */
		public ILocationReader resolve(final URI href, final IErrorHandler errors) throws ResolutionException;
	}
	
	/**
	 * Given a resolver and a root address, construct a map which contains all of the included
	 * things by URI that were found in the root document or any of its includes, and so on.
	 * 
	 * @param resolver
	 * @param root
	 * @param errors
	 * @return
	 */
	public static Map<URI, String> collect(final IResolver resolver, final URI root, final IErrorHandler errors) {
		final HashMap<URI, String> builder = new HashMap<>();
		
		final Deque<URI> addrs = new LinkedList<>();		
		final Deque<Location> includeLocation = new LinkedList<>();

		addrs.add(root);
		includeLocation.add(null);
		URI addr;
		
		/**
		 * Holds a single boolean, which indicates whether we want to recurse into no-includes when
		 * looking for includes to stick in the output.
		 */
		final boolean[] shouldLookWithinNoInclude = new boolean[] {true};
		
		/**
		 * A visitor which pulls out includes and sticks them on the queue to process
		 */
		final INodeVisitor addressCollector = new INodeVisitor(){
				@Override
				public boolean seq(final Seq seq) {
					if (seq.size() >= 1) {
						if (seq.getHead() instanceof Atom) {
							final String nameOfHead = ((Atom)seq.getHead()).getValue(); 
							if (nameOfHead.equals("include") || nameOfHead.equals("include-modules")) {
								URI addr;
								try {
									addr = resolver.convert(seq, errors);
								} catch (final ResolutionException e) {
									return false;
								}
							
								if (builder.containsKey(addr)) {
								} else {
									addrs.push(addr);
									includeLocation.push(seq.getLocation());
								}
								return false;
							} else if (nameOfHead.equals("no-include")) {
								return shouldLookWithinNoInclude[0];
							}
						}
					}
				
					return true;
				}
		
				@Override
				public void comment(final Comment comment) {}
		
				@Override
				public void atom(final Atom atom) {}
			};
		
		while ((addr = addrs.poll()) != null) {
			try {
				final Location iloc = includeLocation.poll();
				final ILocationReader loc = resolver.resolve(addr, errors);
				String stringValue;
				stringValue = IOUtils.toString(loc.getReader());
				builder.put(addr, stringValue);
		
				final List<Node> nodes = Node.copyAll(Parser.source(iloc, loc.getLocation(), new StringReader(stringValue), errors));
			
				for (final Node n : nodes) {
					n.accept(addressCollector);
				}
				
				shouldLookWithinNoInclude[0] = false;
			} catch (final IOException|ResolutionException|UnsupportedOperationException|UnfinishedExpressionException e) {
				errors.handle(BasicError.nowhere(e.getMessage()));
			}
		}
		
		return ImmutableMap.copyOf(builder);
	}
	
	/**
	 * Given a resolver and a root address, make an S-Expression by following all the includes.
	 * @param resolver
	 * @param root
	 * @param errors
	 * @return
	 */
	public static ISExpression source(final IResolver resolver, final URI root, final IErrorHandler errors) {
		return new ISExpression() {
			@Override
			public void accept(final ISExpressionVisitor visitor) {
				try {
					final ILocationReader reader = resolver.resolve(root, errors);
					final ISExpression real = Parser.source(reader.getLocation(), reader.getReader(), errors);
					real.accept(new IncludingVisitor(resolver, new ModuleFilteringVisitor(visitor), errors));
				} catch (final ResolutionException nse) {
					errors.handle(BasicError.nowhere("Unable to resolve" + root + " (" + nse.getMessage() + ")"));
				}
			}
			
			@Override
			public String toString() {
				return root.toString();
			}
		};
	}

	static class ModuleFilteringVisitor extends Editor {
		private boolean enableFilter = false;
		public ModuleFilteringVisitor(final ISExpressionVisitor visitor) {
			super(visitor);
		}

		public void setEnableFilter(final boolean enableFilter) {
			this.enableFilter = enableFilter;
		}

		@Override
		public void atom(final String name) {
			if (editing() ||
				afterOpen() ||
				!enableFilter) {
				super.atom(name);
			}
		}
		
		@Override
		protected Action act(final String name) {
			if (enableFilter) {
				switch (name) {
				case "~module":
					return Action.Ignore; // ignore it but pass it through
				default:
					return Action.Remove; // throw it away entirely, it is bad
				}
			} else {
				return Action.Pass;
			}
		}

		@Override
		protected ISExpression edit(final Seq cut) {
			return cut;
		}
	}
	
	static class IncludingVisitor extends Editor {
		private final IResolver resolver;
		private final IErrorHandler errors;
		private final Stack<URI> stack = new Stack<>();
		private final ModuleFilteringVisitor delegate;
		private int filterModules = 0;

		private IncludingVisitor(final IResolver resolver, final ModuleFilteringVisitor visitor, final IErrorHandler errors) {
			super(visitor);
			this.delegate = visitor;
			this.resolver = resolver;
			this.errors = errors;
		}

		protected boolean isFilteringModules() {
			return filterModules > 0;
		}
		
		@Override
		protected Action act(final String name) {
			switch (name) {
			case "include":
			case "include-modules":
				return Action.RecursiveEdit;
			case "no-include":
				if (stack.isEmpty()) {
					return Action.RecursiveEdit;
				} else {
					return Action.Remove;
				}
			default:
				return Action.Pass;
			}
		}
		
		@Override
		protected ISExpression edit(final Seq cut) {
			boolean filterModules = false;
			final Atom head = (Atom) cut.getHead();
			switch (head.getValue()) {
			case "no-include":
				return new ISExpression() {
					@Override
					public void accept(final ISExpressionVisitor visitor) {
						for (final Node n : cut.getTail()) {
							n.accept(visitor);
						}
					}
				};
			case "include-modules":
				filterModules = true;
			case "include":
			default:
				try {
					final URI uri = resolver.convert(cut, errors);
					
					if (stack.contains(uri)) {
						if (!isFilteringModules()) {
							errors.handle(BasicError.at(cut, uri + " recursively includes itself"));
						}
					} else {
						final ILocationReader reader = resolver.resolve(uri, errors);
						final ISExpression real = 
							Parser.source(cut.getLocation(),
										  reader.getLocation(), 
										  reader.getReader(), 
										  errors);
						
						// this is a bit hacky
						stack.push(uri);
						if (filterModules) {
							this.filterModules++;
							delegate.setEnableFilter(isFilteringModules());
						}
						real.accept(this);
						if (filterModules) {
							this.filterModules--;
							delegate.setEnableFilter(isFilteringModules());
						}
						stack.pop();
					}
					locate(cut.getEndLocation());
					
				} catch (final ResolutionException e) {
					errors.handle(BasicError.at(cut, "Unable to resolve include - " + e.getMessage()));
				}
				break;
			}
			
			return SExpressions.empty();
		}
	}
		

	public static final URI root = URI.create("root:root");
	
	/**
	 * Recursively collect all the includes starting from the given root provided as a string 
	 * @param scenarioService
	 * @param scenarioXML
	 * @param slf4j
	 * @return
	 */
	public static Map<URI, String> collectFromRoot(final IResolver resolver, 
												   final String scenarioXML,
												   final IErrorHandler errors) {
		
		return collect(new IResolver(){
				@Override
				public ILocationReader resolve(final URI href, final IErrorHandler errors)
					throws ResolutionException {
					if (href == root) return stringLocationReader(root, scenarioXML);
					else return resolver.resolve(href, errors);
				}
		
				@Override
				public URI convert(final Seq include, final IErrorHandler errors) throws ResolutionException {
					return resolver.convert(include, errors);
				}
			}, root, errors);
	}
}
