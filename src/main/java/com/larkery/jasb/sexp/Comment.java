package com.larkery.jasb.sexp;

public class Comment extends Node {
	private final String text;
	Comment(Location location, final String text) {
		super(location);
		this.text = text;
	}
	@Override
	public String toString() {
		return String.format(" ;; %s\n", text);
	}
	@Override
	public void accept(ISexpVisitor visitor) {
		super.accept(visitor);
		visitor.comment(text);
	}
	@Override
	public void accept(INodeVisitor visitor) {
		visitor.comment(this);;
	}
}
