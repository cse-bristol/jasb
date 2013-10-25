package com.larkery.jasb.sexp;

import java.util.Stack;

import com.larkery.jasb.sexp.Seq.Builder;

public class NodeBuilder implements ISexpVisitor {
	private Location here;
	private final Stack<Seq.Builder> inprogress = new Stack<>();
	private Builder top;
	
	public NodeBuilder() {
		top = Seq.builder(null);
		inprogress.push(top);
	}
	
	@Override
	public void open() {
		inprogress.add(Seq.builder(here));
	}
	
	@Override
	public void locate(Location loc) {
		here = loc;
	}
	
	@Override
	public void close() {
		final Seq seq = inprogress.pop().build(here);
		inprogress.peek().add(seq);
	}
	
	@Override
	public void atom(String string) {
		inprogress.peek().add(new Atom(here, string));
	}
	
	public Node get() {
		return top.build(null).getHead();
	}
}
