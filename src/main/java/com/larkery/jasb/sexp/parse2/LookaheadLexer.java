package com.larkery.jasb.sexp.parse2;

import java.util.LinkedList;

import com.google.common.base.Optional;
import com.larkery.jasb.sexp.Location;
import com.larkery.jasb.sexp.parse2.Lexer.Lexeme;

public class LookaheadLexer {
	private final Lexer delegate;
	private final LinkedList<Lexeme> buffer = new LinkedList<>();
	
	public LookaheadLexer(final Lexer delegate) {
		super();
		this.delegate = delegate;
	}

	public boolean hasNext() {
		return !buffer.isEmpty() || delegate.hasNext();
	}
	
	public Lexeme next() {
		if (lookAhead(0).isPresent()) {
			return buffer.remove();
		} else {
			return null;
		}
	}
	
	public Optional<Lexeme> lookAhead(final int count) {
		while (buffer.size() <= count && delegate.hasNext()) {
			buffer.add(delegate.next());
		}
		
		if (buffer.size() > count) {
			return Optional.of(buffer.get(count));
		} else {
			return Optional.absent();
		}
	}

	public void setSeparateColons(final boolean b) {
		// but wait what if we looked ahead? we need to re-cut those bits
		delegate.setSeparateColons(b);
	}

	public Location location() {
		return delegate.location();
	}
}
