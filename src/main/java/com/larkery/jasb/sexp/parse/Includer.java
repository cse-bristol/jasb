package com.larkery.jasb.sexp.parse;

import java.io.Reader;

import com.google.common.base.Optional;
import com.larkery.jasb.sexp.ISexpSource;
import com.larkery.jasb.sexp.ISexpVisitor;
import com.larkery.jasb.sexp.Location.Type;
import com.larkery.jasb.sexp.NodeBuilder;
import com.larkery.jasb.sexp.Seq;
import com.larkery.jasb.sexp.errors.IErrorHandler;

public class Includer {
	public interface ILocationReader {
		public Reader getReader();
		public String getLocation();
	}
	
	public interface IResolver {
		public ILocationReader resolve(final Seq include, final IErrorHandler errors);
		public ILocationReader resolve(final String root, final IErrorHandler errors);
	}
	
	public static ISexpSource source(final IResolver resolver, final String root, final IErrorHandler errors) {
		return new ISexpSource() {
			@Override
			public void accept(final ISexpVisitor visitor) {
				final ILocationReader reader = resolver.resolve(root, errors);
				final ISexpSource real = Parser.source(Type.Normal, reader.getLocation(), reader.getReader(), errors);
				
				real.accept(new IncludingVisitor(resolver, visitor, errors));
			}
			
			@Override
			public String toString() {
				return root;
			}
		};
	}
	
	static class IncludingVisitor extends Cutout<NodeBuilder> {
		private final IResolver resolver;
		private final IErrorHandler errors;
		
		private IncludingVisitor(final IResolver resolver, final ISexpVisitor visitor, final IErrorHandler errors) {
			super(visitor);
			this.resolver = resolver;
			this.errors = errors;
		}

		@Override
		protected Optional<NodeBuilder> cut(final String head) {
			if (head.equals("include")) {
				return Optional.of(new NodeBuilder());
			} else {
				return Optional.absent();
			}
		}
		
		@Override
		protected void paste(final NodeBuilder q) {
			final ILocationReader reader = resolver.resolve((Seq) q.get(), errors);
			final ISexpSource real = Parser.source(Type.Include, reader.getLocation(), reader.getReader(), errors);
			real.accept(this);
		}
	}
}
