package com.larkery.jasb.sexp;

import java.util.Stack;

import com.larkery.jasb.sexp.Seq.Builder;

public class NodeBuilder implements ISExpressionVisitor {
	private Location here;
	private final Stack<Seq.Builder> inprogress = new Stack<>();
	private final Builder top;
	private final boolean includeComments;
	
	private NodeBuilder(final boolean includeComments) {
		this.includeComments = includeComments;
		top = Seq.builder(null);
		inprogress.push(top);
	}
	
	public static NodeBuilder create() {
		return new NodeBuilder(true);
	}
	
	public static NodeBuilder withoutComments() {
		return new NodeBuilder(true);
	}
	
	@Override
	public void open() {
		inprogress.add(Seq.builder(here));
	}
	
	@Override
	public void locate(final Location loc) {
		here = loc;
	}
	
	@Override
	public void close() {
		final Seq seq = inprogress.pop().build(here);
		inprogress.peek().add(seq);
	}
	
	@Override
	public void atom(final String string) {
		inprogress.peek().add(new Atom(here, string));
	}
	
	@Override
	public void comment(final String text) {
		if (includeComments) {
			inprogress.peek().add(new Comment(here, text));
		}
	}
	
	public Node get() {
		if (inprogress.size() > 1) {
			throw new UnsupportedOperationException("there are " + inprogress.size() + " elements left on the stack (" +inprogress+ ")");
		}
		return top.build(null).getHead();
	}
}
