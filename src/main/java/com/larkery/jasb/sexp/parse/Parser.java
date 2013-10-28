package com.larkery.jasb.sexp.parse;

import java.io.IOException;
import java.io.Reader;

import com.google.common.base.CharMatcher;
import com.larkery.jasb.sexp.ISexpSource;
import com.larkery.jasb.sexp.ISexpVisitor;
import com.larkery.jasb.sexp.Location;
import com.larkery.jasb.sexp.errors.BasicError;
import com.larkery.jasb.sexp.errors.IErrorHandler;

public class Parser {
	private final StripComments reader;
	private StringBuffer atombuilder = null;
	private Location atomlocation;
	private final String locationName;
	private Location here;
	int depth = 0;
	
	private Parser(final String location, final Reader reader) {
		super();
		this.locationName = location;
		this.reader = new StripComments(reader);
	}

	enum S {
		Free,
		InString,
		Escaped
	}
	
	public static void parse(final String location, final Reader input, final ISexpVisitor output, final IErrorHandler errors) throws IOException {
		new Parser(location, input).parse(output, errors);
	}
	
	public static ISexpSource source(final String location, final Reader input, final IErrorHandler errors) {
		return new ISexpSource() {
			@Override
			public void accept(ISexpVisitor visitor) {
				try {
					parse(location, input, visitor, errors);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
			
			@Override
			public String toString() {
				return location;
			}
		};
	}
	
	private void parse(final ISexpVisitor output, IErrorHandler errors) throws IOException {
		int input;
		
		S state = S.Free;
		
		here = location();
		
		while ((input = reader.read()) != -1) {
			final char c = (char) input;
			switch (state) {
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
	
	private void extendAtom(char c) {
		startAtom();
		atombuilder.append(c);
	}
	
	private void sendAtom(ISexpVisitor output) {
		if (atombuilder != null) {
			output.locate(atomlocation);
			output.atom(atombuilder.toString());
			atombuilder = null;
		}
	}

	private Location location() {
		return Location.of(locationName, reader.offset, reader.line, reader.column);
	}
}
