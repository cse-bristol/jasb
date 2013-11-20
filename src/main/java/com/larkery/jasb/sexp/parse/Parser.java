package com.larkery.jasb.sexp.parse;

import java.io.IOException;
import java.io.Reader;

import com.google.common.base.CharMatcher;
import com.larkery.jasb.sexp.ISExpression;
import com.larkery.jasb.sexp.ISExpressionVisitor;
import com.larkery.jasb.sexp.Location;
import com.larkery.jasb.sexp.Location.Type;
import com.larkery.jasb.sexp.errors.BasicError;
import com.larkery.jasb.sexp.errors.IErrorHandler;

public class Parser {
	private final Reader reader;
	
	private final String locationName;
	
	private long offset, line, column;
	private final Type type;
	
	private Parser(final Type type, final String location, final Reader reader) {
		super();
		this.type = type;
		this.locationName = location;
		this.reader = reader;
	}
	
	public static void parse(final Location.Type type, final String location, final Reader input, final ISExpressionVisitor output, final IErrorHandler errors) throws IOException {
		new Parser(type, location, input).parse(output, errors);
	}
	
	public static ISExpression source(final Location.Type type, final String location, final Reader input, final IErrorHandler errors) {
		return new ISExpression() {
			@Override
			public void accept(final ISExpressionVisitor visitor) {
				try {
					parse(type, location, input, visitor, errors);
				} catch (final IOException e) {
					throw new RuntimeException(e);
				}
			}
			
			@Override
			public String toString() {
				return location;
			}
		};
	}
	
	static abstract class ParseState {
		int depth = 0;
		
		public abstract ParseState next(
				final Location here,
				final char c, 
				final ISExpressionVisitor output, 
				final IErrorHandler errors);
		
		public abstract void complete(
				Location here,
				ISExpressionVisitor output, 
				IErrorHandler errors);
		
		public static ParseState start() {
			return new Whitespace();
		}

		public boolean isError() {
			return false;
		}
		
		static class Comment extends ParseState {
			private final Location here;
			private final Whitespace whitespace;
			private final StringBuffer buffer = new StringBuffer();

			public Comment(final Whitespace whitespace, final Location here) {
				this.whitespace = whitespace;
				this.here = here;
			}
			
			@Override
			public void complete(final Location here, final ISExpressionVisitor output, final IErrorHandler errors) {
				send(output);
				whitespace.complete(here, output, errors);
			}
			
			private void send(final ISExpressionVisitor output) {
				output.locate(here);
				output.comment(buffer.toString());
			}

			@Override
			public ParseState next(final Location here, final char c,final ISExpressionVisitor output, final IErrorHandler errors) {
				if (c=='\r' || c=='\n') {
					send(output);
					return whitespace;
				} else {
					buffer.append(c);
					return this;
				}
			}
		}
		
		static class Error extends ParseState {
			@Override
			public ParseState next(final Location here, final char c,
					final ISExpressionVisitor output, final IErrorHandler errors) {return this;}

			@Override
			public void complete(final Location here, final ISExpressionVisitor output,
					final IErrorHandler errors) {}
			@Override
			public boolean isError() {
				return true;
			}
		}
		
		static class Atom extends ParseState {
			private final Whitespace whitespace;
			private final StringBuffer buffer = new StringBuffer();
			boolean isQuoted;
			boolean isEscaping = false;
			private final Location location;

			public Atom(final Whitespace whitespace, final char start, final Location location) {
				this.whitespace = whitespace;
				this.location = location;
				isQuoted = start == '"';
				if (!isQuoted) buffer.append(start);
			}

			@Override
			public ParseState next(
					final Location here, 
					final char c,
					final ISExpressionVisitor output, 
					final IErrorHandler errors) {
				
				if (isQuoted) {
					if (isEscaping) {
						buffer.append(c);
						isEscaping = false;
						return this;
					} else {
						switch (c) {
						case '"':
							return send(output);
						case '\\':
							isEscaping = true;
							return this;
						default:
							buffer.append(c);
							return this;
						}
					}
				} else {
					switch (c) {
					case ':':
						buffer.append(c);
						return send(output);
					case '(':
					case ')':
					case ';':
					case '"':
						return send(output).next(here, c, output, errors);
					default:
						if (CharMatcher.WHITESPACE.matches(c)) {
							return send(output).next(here, c, output, errors);
						} else {
							buffer.append(c);
							return this;
						}
					}
				}
			}

			@Override
			public void complete(final Location here, final ISExpressionVisitor output, final IErrorHandler errors) {
				send(output);
				whitespace.complete(here, output, errors);
			}
			
			private ParseState send(final ISExpressionVisitor output) {
				output.locate(location);
				output.atom(buffer.toString());
				return whitespace;
			}
		}
		
		static class Whitespace extends ParseState {
			@Override
			public ParseState next(
					final Location here, 
					final char c,
					final ISExpressionVisitor output, 
					final IErrorHandler errors) {
				if (CharMatcher.WHITESPACE.matches(c)) {
					return this;
				} else {
					switch (c) {
					case ';': return new Comment(this, here);
					case '(':
						depth++;
						output.open();
						return this;
					case ')':
						depth--;
						if (depth < 0) {
							errors.handle(BasicError.at(here, "Too many closing parentheses"));
							return new Error();
						}
						output.close();
						return this;
					default:
						return new Atom(this, c, here);
					}
				}
			}

			@Override
			public void complete(final Location here, final ISExpressionVisitor output, final IErrorHandler errors) {
				if (depth != 0) {
					errors.handle(BasicError.at(here, "Expected " + depth + " more parentheses"));
				}
			}
		}
	}
	
	private void parse(final ISExpressionVisitor output, final IErrorHandler errors) throws IOException {
		int input;
		
		ParseState state = ParseState.start();
	
		offset = line = column = 0;
	
		while ((input = reader.read()) != -1 && !state.isError()) {
			state = state.next(location(), (char) input, output, errors);
			offset++;
			column++;
			if (input == '\n' || input == '\r') {
				line++;
				column = 0;
			}
		}
		
		state.complete(location(), output, errors);
	}
	
	private Location location() {
		return Location.of(type, locationName, offset, line, column);
	}
}
