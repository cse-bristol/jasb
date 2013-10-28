package com.larkery.jasb.sexp;

import com.google.common.base.CharMatcher;

public class Atom extends Node {
	private final String value;
	private final boolean quoted;

	Atom(Location location, String value) {
		super(location);
		this.value = value;
		this.quoted = CharMatcher.WHITESPACE.matchesAnyOf(value);
	}

	
	public String getValue() {
		return value;
	}
	
	public boolean isQuoted() {
		return quoted;
	}
	
	@Override
	public void accept(ISexpVisitor visitor) {
		super.accept(visitor);
		visitor.atom(value);
	}
	
	@Override
	public String toString() {
		if (quoted) {
			return "\"" + value.replace("\"", "\\\"") + "\"";
		} else {
			return value;
		}
	}
	
	@Override
	public void accept(INodeVisitor visitor) {
		visitor.atom(this);
	}
}
