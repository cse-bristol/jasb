package com.larkery.jasb.sexp;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import com.google.common.collect.ImmutableList;


public class Seq extends Node implements Iterable<Node> {
	private final List<Node> nodes;
	private final Location end;
	
	private Seq(Location location, Location end, List<Node> nodes) {
		super(location);
		this.end = end;
		this.nodes = ImmutableList.copyOf(nodes);
	}

	public Node get(int arg0) {
		return nodes.get(arg0);
	}

	public boolean isEmpty() {
		return nodes.isEmpty();
	}

	public Iterator<Node> iterator() {
		return nodes.iterator();
	}
	
	public List<Node> getTail() {
		return nodes.subList(1, nodes.size());
	}
	
	public Node getHead() {
		if (nodes.isEmpty()) {
			throw new NoSuchElementException("tried to get head element of empty s-expression");
		}
		return nodes.get(0);
	}
	
	@Override
	public void accept(final ISexpVisitor visitor) {
		super.accept(visitor);
		visitor.open();
		for (final Node node : this) {
			node.accept(visitor);
		}
		visitor.locate(end);
		visitor.close();
	}
	
	public int size() {
		return nodes.size();
	}

	@Override
	public String toString() {
		final StringBuffer sb = new StringBuffer();
		sb.append("(");
		boolean space = false;
		for (final Node n : this) {
			if (space) sb.append(" ");
			else space = true;
			sb.append(n);
		}
		sb.append(")");
		return sb.toString();
	}

	public static class Builder {
		private final Location start;
		private final ImmutableList.Builder<Node> builder = ImmutableList.builder();
		
		private Builder(Location start) {
			super();
			this.start = start;
		}

		public void add(final Node node) {
			builder.add(node);
		}
		
		public Seq build(final Location end) {
			return new Seq(start, end, this.builder.build());
		}
	}
	
	public static Builder builder(Location start) {
		return new Builder(start);
	}
	
	public List<Node> getNodes() {
		return nodes;
	}
	
	@Override
	public void accept(INodeVisitor visitor) {
		visitor.seq(this);
		for (final Node n : nodes) n.accept(visitor);
	}
}
