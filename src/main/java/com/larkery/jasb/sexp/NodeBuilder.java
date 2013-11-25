package com.larkery.jasb.sexp;

import java.util.Stack;

import com.larkery.jasb.sexp.Seq.Builder;

public class NodeBuilder implements ISExpressionVisitor {
	private Location here;
	private final Stack<Seq.Builder> inprogress = new Stack<>();
	private final Builder top;
	private Node lastNode;
	private final boolean includeComments;
	
	protected NodeBuilder(final boolean includeComments) {
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
		push(seq);
	}

	private void push(final Node seq) {
		inprogress.peek().add(seq);
		lastNode = seq;
	}
	
	@Override
	public void atom(final String string) {
		push(new Atom(here, string));
	}
	
	@Override
	public void comment(final String text) {
		if (includeComments) {
			push(new Comment(here, text));
		}
	}
	
	public Node get() {
		if (inprogress.size() > 1) {
			throw new UnsupportedOperationException("there are " + inprogress.size() + " elements left on the stack (" +inprogress+ ")");
		}
		return top.build(null).getHead();
	}

	/**
	 * @return the last node created by an event this saw
	 */
	public Node getLastNode() {
		return lastNode;
	}
}
