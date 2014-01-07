package com.larkery.jasb.sexp.parse;

import java.io.IOException;
import java.io.Reader;
import java.net.URI;

import com.google.common.base.CharMatcher;
import com.larkery.jasb.sexp.Delim;
import com.larkery.jasb.sexp.ISExpression;
import com.larkery.jasb.sexp.ISExpressionVisitor;
import com.larkery.jasb.sexp.Location;
import com.larkery.jasb.sexp.Location.Position;
import com.larkery.jasb.sexp.Location.Type;
import com.larkery.jasb.sexp.errors.BasicError;
import com.larkery.jasb.sexp.errors.IErrorHandler;
import java.util.List;
import com.google.common.collect.ImmutableList;
import java.util.Collections;

public class Parser {
	private final Reader reader;
	
	private final URI locationName;
	
	private long offset;
	private int line, column;
	private final Type type;
	private final List<Position> positions;
	
	private Parser(final List<Position> positions, final Type type, final URI location, final Reader reader) {
		super();
		this.type = type;
		this.locationName = location;
		this.reader = reader;
		this.positions = positions;
	}
	
	public static void parse(final List<Position> positions, final Location.Type type, final URI location, final Reader input, final ISExpressionVisitor output, final IErrorHandler errors) throws IOException {
		new Parser(positions, type, location, input).parse(output, errors);
	}
	
	public static ISExpression source(final URI location, final Reader input, final IErrorHandler errors) {
		return source(Collections.<Position>emptyList(), Location.Type.Normal, location, input, errors);
	}

	public static ISExpression source(final List<Position> positions, final Location.Type type, final URI location, final Reader input, final IErrorHandler errors) {
		return new ISExpression() {
			@Override
			public void accept(final ISExpressionVisitor visitor) {
				try {
					parse(positions, type, location, input, visitor, errors);
				} catch (final IOException e) {
					throw new RuntimeException(e);
				}
			}
			
			@Override
			public String toString() {
				return location.toString();
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
						if (buffer.length() == 0) {
							return this;
						} else {
							return send(output);
						}
					case '(':
					case ')':
					case '[':
					case ']':
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
					case '[':
						depth++;
						output.locate(here);
						output.open(Delim.of(c));
						return this;
					case ')':
					case ']':
						depth--;
						if (depth < 0) {
							errors.handle(BasicError.at(here, "Too many closing parentheses"));
							return new Error();
						}
						output.locate(here);
						output.close(Delim.of(c));
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
	
		offset = line = column = 1;
	
		while ((input = reader.read()) != -1 && !state.isError()) {
			offset++;
			column++;
			if (input == '\n') {
				line++;
				column = 0;
			}
			state = state.next(location(), (char) input, output, errors);
		}
		
		state.complete(location(), output, errors);
	}
	
	private Location location() {
		return Location.of(type, 
						   ImmutableList.<Position>builder().addAll(positions).add(Location.Position.of(locationName, line, column)).build());
	}
}
