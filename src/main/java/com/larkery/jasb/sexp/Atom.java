package com.larkery.jasb.sexp;

import org.pojomatic.Pojomatic;
import org.pojomatic.annotations.AutoDetectPolicy;
import org.pojomatic.annotations.AutoProperty;
import org.pojomatic.annotations.PojomaticPolicy;
import org.pojomatic.annotations.Property;

import com.google.common.base.CharMatcher;

@AutoProperty(autoDetect=AutoDetectPolicy.NONE)
public class Atom extends Node {
	private final String value;
	private final boolean quoted;

	Atom(final Location location, final String value) {
		super(location);
		this.value = value;
		this.quoted = CharMatcher.WHITESPACE.matchesAnyOf(value) || value.isEmpty();
	}

	@Property(policy=PojomaticPolicy.HASHCODE_EQUALS)
	public String getValue() {
		return value;
	}
	
	public boolean isQuoted() {
		return quoted;
	}
	
	@Override
	public void accept(final ISExpressionVisitor visitor) {
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
	public void accept(final INodeVisitor visitor) {
		visitor.atom(this);
	}
	
	@Override
	public boolean equals(final Object obj) {
		return Pojomatic.equals(this, obj);
	}
	
	@Override
	public int hashCode() {
		return Pojomatic.hashCode(this);
	}

	public static Node create(final String string) {
		return new Atom(null,string);
	}
}
