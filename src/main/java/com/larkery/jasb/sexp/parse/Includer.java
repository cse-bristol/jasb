package com.larkery.jasb.sexp.parse;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.net.URI;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.NoSuchElementException;

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

public class Includer {
	private static final Logger log = LoggerFactory.getLogger(Includer.class);
	
	public interface ILocationReader {
		public Reader getReader();
		public URI getLocation();
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
		 */
		public URI convert(final Seq include, final IErrorHandler errors);
		
		/**
		 * Get the content pointed to by a URI produced by {@link #convert(Seq, IErrorHandler)}
		 * 
		 * @param href
		 * @param errors
		 * @return
		 * @throws NoSuchElementException
		 */
		public ILocationReader resolve(final URI href, final IErrorHandler errors) throws NoSuchElementException;
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
							final URI addr = resolver.convert(seq, errors);
							if (builder.containsKey(addr)) {
								errors.handle(BasicError.at(seq, "Recursive includes are illegal"));
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
			final ILocationReader loc = resolver.resolve(addr, errors);
			String stringValue;
			try {
				stringValue = IOUtils.toString(loc.getReader());
				final Node node = Node.copy(Parser.source(type, loc.getLocation(), new StringReader(stringValue), errors));
				type = Type.Include;
				if (node != null) {
					builder.put(addr, stringValue);
					node.accept(addressCollector);
				}
			} catch (final IOException e) {
				
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
				} catch (final NoSuchElementException nse) {
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
		
		private IncludingVisitor(final IResolver resolver, final ISExpressionVisitor visitor, final IErrorHandler errors) {
			super(visitor);
			this.resolver = resolver;
			this.errors = errors;
		}

		@Override
		protected Optional<NodeBuilder> cut(final String head) {
			if (head.equals("include")) {
				log.debug("cutting out include");
				return Optional.of(new NodeBuilder());
			} else {
				return Optional.absent();
			}
		}
		
		@Override
		protected void paste(final NodeBuilder q) {
			log.debug("pasting completed include");
			try {
				final ILocationReader reader = resolver.resolve(
						resolver.convert((Seq) q.get(), errors), errors);
				final ISExpression real = Parser.source(
						Type.Include, reader.getLocation(), reader.getReader(), errors);
				real.accept(this);
			} catch (final NoSuchElementException e) {
				log.error("Error resolving a scenario from {}", q.get(), e);
				errors.handle(BasicError.at(q.get(), "Unable to resolve include - " + e.getMessage()));
			}
		}
	}
}
