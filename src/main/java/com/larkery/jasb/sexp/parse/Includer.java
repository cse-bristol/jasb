package com.larkery.jasb.sexp.parse;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.net.URI;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Stack;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.larkery.jasb.sexp.Atom;
import com.larkery.jasb.sexp.Comment;
import com.larkery.jasb.sexp.INodeVisitor;
import com.larkery.jasb.sexp.ISExpression;
import com.larkery.jasb.sexp.ISExpressionVisitor;
import com.larkery.jasb.sexp.Location.Type;
import com.larkery.jasb.sexp.Node;
import com.larkery.jasb.sexp.NodeBuilder;
import com.larkery.jasb.sexp.Seq;
import com.larkery.jasb.sexp.errors.BasicError;
import com.larkery.jasb.sexp.errors.IErrorHandler;
import com.larkery.jasb.sexp.errors.ResolutionException;
import com.larkery.jasb.sexp.errors.UnfinishedExpressionException;

public class Includer {
	private static final Logger log = LoggerFactory.getLogger(Includer.class);
	
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
		addrs.add(root);
		URI addr;
		
		/**
		 * A visitor which pulls out includes and stucks them on the queue to process
		 */
		final INodeVisitor addressCollector = new INodeVisitor(){
			@Override
			public boolean seq(final Seq seq) {
				if (seq.size() >= 1) {
					if (seq.getHead() instanceof Atom) {
						if (((Atom)seq.getHead()).getValue().equals("include")) {
							URI addr;
							try {
								addr = resolver.convert(seq, errors);
							} catch (final ResolutionException e) {
								return false;
							}
							if (builder.containsKey(addr)) {
							} else {
								addrs.push(addr);
							}
							return false;
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
		
		Type type = Type.Normal;
		while ((addr = addrs.poll()) != null) {
			try {
			final ILocationReader loc = resolver.resolve(addr, errors);
			String stringValue;
			stringValue = IOUtils.toString(loc.getReader());
			builder.put(addr, stringValue);
		
			final Node node = Node.copy(Parser.source(type, loc.getLocation(), new StringReader(stringValue), errors));
			
			type = Type.Include;
			if (node != null) {
				node.accept(addressCollector);
			}
			} catch (final IOException e) {
			} catch (final ResolutionException re) {
				log.warn("Unable to resolve scenario {}", addr, re);
			} catch (final UnsupportedOperationException e) {
			} catch (final UnfinishedExpressionException e) {
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
					final ISExpression real = Parser.source(Type.Normal, reader.getLocation(), reader.getReader(), errors);
					real.accept(new IncludingVisitor(resolver, visitor, errors));
				} catch (final ResolutionException nse) {
					log.error("Error resolving a scenario from {}", root, nse);
					errors.handle(BasicError.nowhere("Unable to resolve" + root + " (" + nse.getMessage() + ")"));
				}
			}
			
			@Override
			public String toString() {
				return root.toString();
			}
		};
	}
	
	static class IncludingVisitor extends Cutout<NodeBuilder> {
		private final IResolver resolver;
		private final IErrorHandler errors;
		private final Stack<URI> stack = new Stack<>();
		
		private IncludingVisitor(final IResolver resolver, final ISExpressionVisitor visitor, final IErrorHandler errors) {
			super(visitor);
			this.resolver = resolver;
			this.errors = errors;
		}

		@Override
		protected Optional<NodeBuilder> cut(final String head) {
			if (head.equals("include")) {
				log.debug("cutting out include");
				return Optional.of(NodeBuilder.create());
			} else {
				return Optional.absent();
			}
		}
		
		@Override
		protected void paste(final NodeBuilder q) {
			log.debug("pasting completed include");
			final Node node = q.getBestEffort();
			try {
				if (node instanceof Seq) {
					final Seq include = (Seq) node;
					final URI uri = resolver.convert(include, errors);
					
					if (stack.contains(uri)) {
						errors.handle(BasicError.at(include, uri + " recursively includes itself"));
					} else {
						final ILocationReader reader = resolver.resolve(uri, errors);
						final ISExpression real = Parser.source(
								Type.Include, reader.getLocation(), reader.getReader(), errors);
						stack.push(uri);
						real.accept(this);
						stack.pop();
					}
					locate(include.getEndLocation());
				}
			} catch (final ResolutionException e) {
				log.error("Error resolving a scenario from {}", node, e);
				errors.handle(BasicError.at(node, "Unable to resolve include - " + e.getMessage()));
			}
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
	public static Map<URI, String> collectFromRoot(
			final IResolver resolver, 
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
