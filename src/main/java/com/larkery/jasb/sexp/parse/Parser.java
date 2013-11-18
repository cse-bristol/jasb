package com.larkery.jasb.sexp.parse;

import java.io.IOException;
import java.io.Reader;

import com.google.common.base.CharMatcher;
import com.larkery.jasb.sexp.ISexpSource;
import com.larkery.jasb.sexp.ISexpVisitor;
import com.larkery.jasb.sexp.Location;
import com.larkery.jasb.sexp.Location.Type;
import com.larkery.jasb.sexp.errors.BasicError;
import com.larkery.jasb.sexp.errors.IErrorHandler;

public class Parser {
	private final Reader reader;
	private StringBuffer atombuilder = null;
	private StringBuffer commentbuilder = null;
	private Location atomlocation, commentlocation;
	private final String locationName;
	private Location here;
	int depth = 0;
	private long offset, line, column;
	private final Type type;
	
	private Parser(final Type type, final String location, final Reader reader) {
		super();
		this.type = type;
		this.locationName = location;
		this.reader = reader;
	}

	enum S {
		Free,
		InString,
		Escaped,
		Semicolon,
		Comment
	}
	
	public static void parse(final Location.Type type, final String location, final Reader input, final ISexpVisitor output, final IErrorHandler errors) throws IOException {
		new Parser(type, location, input).parse(output, errors);
	}
	
	public static ISexpSource source(final Location.Type type, final String location, final Reader input, final IErrorHandler errors) {
		return new ISexpSource() {
			@Override
			public void accept(final ISexpVisitor visitor) {
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
	
	private void parse(final ISexpVisitor output, final IErrorHandler errors) throws IOException {
		int input;
		
		S previousState = S.Free;
		S state = S.Free;
		
		here = location();
		
		while ((input = reader.read()) != -1) {
			final char c = (char) input;
			
			offset++;
			if (c == '\n') {
				line++;
				column = 0;
			} else {
				column++;
			}
			
			// if we see a semicolon, we need to look at that and not worry about the rest
			if (c == ';') {
				if (state == S.Semicolon) {
					sendAtom(output);
					state = S.Comment;
				} else {
					previousState = state;
					state = S.Semicolon;
				}
				continue;
			}
			// if we have seen a semicolon, we can play it out and go back to previous state
			if (state == S.Semicolon) {
				extendAtom(';');
				state = previousState;
			}
			switch (state) {
			case Semicolon:
				break;
			case Comment:
				// append char to comment buffer
				if (c == '\n') {
					state = previousState;
					sendComment(output);
				} else {
					extendComment(c);
				}
				break;
			case Free:
				if (CharMatcher.WHITESPACE.matches(c)) {
					sendAtom(output);
				} else if (c == ':') {
					extendAtom(c);
					sendAtom(output);
				} else if (c == '(') {
					sendAtom(output);
					output.locate(here);
					depth++;
					output.open();
				} else if (c == ')') {
					sendAtom(output);
					output.locate(here);
					if (depth == 0) {
						errors.handle(BasicError.at(here, "Too many closing parentheses"));
						return;
					}
					depth--;
					output.close();
				} else if (c == '"') {
					sendAtom(output);
					state = S.InString;
					startAtom();
				} else {
					extendAtom(c);
				}
				break;
			case Escaped:
				if (c == '"') {
					extendAtom('"');
				} else {
					extendAtom(c);
				}
				break;
			case InString:
				if (c == '\\') {
					state = S.Escaped;
				} else if (c == '"') {
					sendAtom(output);
				} else {
					extendAtom(c);
				}
				break;
			}
			here = location();
		}
		
		sendComment(output);
		sendAtom(output);
		
		if (depth != 0) {
			errors.handle(BasicError.at(here, "Unclosed parentheses at end of input; expecting " + depth + " more."));
		}
	}

	private void startAtom() {
		if (atombuilder == null) {
			atombuilder = new StringBuffer();
			atomlocation = here;
		}
	}
	
	private void extendAtom(final char c) {
		startAtom();
		atombuilder.append(c);
	}
	
	private void sendAtom(final ISexpVisitor output) {
		if (atombuilder != null) {
			output.locate(atomlocation);
			output.atom(atombuilder.toString());
			atombuilder = null;
		}
	}
	
	
	private void startComment() {
		if (commentbuilder == null) {
			commentbuilder = new StringBuffer();
			commentlocation = here;
		}
	}
	
	private void extendComment(final char c) {
		startComment();
		commentbuilder.append(c);
	}
	
	private void sendComment(final ISexpVisitor output) {
		if (commentbuilder != null) {
			output.locate(commentlocation);
			output.comment(commentbuilder.toString());
			commentbuilder = null;
		}
	}

	private Location location() {
		return Location.of(type, locationName, offset, line, column);
	}
}
