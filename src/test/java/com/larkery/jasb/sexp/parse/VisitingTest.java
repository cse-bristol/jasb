package com.larkery.jasb.sexp.parse;

import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;

import com.larkery.jasb.sexp.ISExpression;
import com.larkery.jasb.sexp.ISExpressionVisitor;
import com.larkery.jasb.sexp.Location;
import com.larkery.jasb.sexp.Location.Type;
import com.larkery.jasb.sexp.NodeBuilder;
import com.larkery.jasb.sexp.PrintVisitor;
import com.larkery.jasb.sexp.errors.IErrorHandler;
import com.larkery.jasb.sexp.errors.IErrorHandler.IError;

public class VisitingTest {
	final IErrorHandler RECORD = new IErrorHandler() {
		@Override
		public void handle(final IError error) {
			errors.add(error);
		}
	};
	
	final Set<IError> errors = new HashSet<>();
	
	@Before
	public void resetErrors() {
		 errors.clear();
	}
	
	static class E implements ISExpressionVisitor {
		final String value;
		enum T {
			Open, Atom, Comment, Close
		}
		final T type;
		
		private E(final String value, final T type) {
			super();
			this.value = value;
			this.type = type;
		}

		@Override
		public void locate(final Location loc) {}

		@Override
		public void open() {
			Assert.assertEquals(T.Open, this.type);
		}

		@Override
		public void atom(final String string) {
			Assert.assertEquals(this.value, string);
			Assert.assertEquals(T.Atom, this.type);
		}

		@Override
		public void comment(final String text) {
			Assert.assertEquals(this.value, text);
			Assert.assertEquals(T.Comment, this.type);
		}

		@Override
		public void close() {
			Assert.assertEquals(T.Close, this.type);
		}
	}
	
	public static E e(final String value) {
		switch (value) {
		case "(":return new E(value, E.T.Open);
		case ")":return new E(value, E.T.Close);
		default:
			if (value.startsWith(";")) return new E(value.substring(1), E.T.Comment);
			else return new E(value, E.T.Atom);
		}
	}
	
	protected ISExpression source(final String name, final String src) {
		return Parser.source(
				Type.Normal,
				createTestURI(name),
				new StringReader(src),
				RECORD);
	}
	
	protected void check(final String name, final String src, final Set<Class<? extends IError>> errorTypes) {
		try {
			source(name, src).accept(new NodeBuilder());
		} finally {
			outer:
			for (final Class<? extends IError> e : errorTypes) {
				for (final IError err : errors) {
					if (e.isInstance(err)) {
						continue outer;
					}
				}
				
				Assert.fail("No error of type " + e.getSimpleName() + " seen");
			}
		}
	}
	
	protected void check(final String name, final String src, final E... values) {
		try {
		source(name, src)
				.accept(new ISExpressionVisitor() {
					int offset = 0;
					@Override
					public void open() {
						values[offset++].open();
					}
					
					@Override
					public void locate(final Location loc) {
					}
					
					@Override
					public void comment(final String text) {
						values[offset++].comment(text);
					}
					
					@Override
					public void close() {
						values[offset++].close();
					}
					
					@Override
					public void atom(final String string) {
						values[offset++].atom(string);
					}
				});
		} catch (final Throwable e) {
				Parser.source(Type.Normal, createTestURI(name), new StringReader(src), IErrorHandler.SLF4J).accept(new PrintVisitor(System.out));
			
			throw e;
		}
	}

	private URI createTestURI(final String name)  {
		try {
			
		return new URI("test", name, null);
	} catch (final URISyntaxException e1) {
		throw new IllegalArgumentException(e1.getMessage(), e1);
	}
	}
}
