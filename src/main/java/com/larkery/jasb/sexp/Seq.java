package com.larkery.jasb.sexp;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.pojomatic.Pojomatic;
import org.pojomatic.annotations.AutoDetectPolicy;
import org.pojomatic.annotations.AutoProperty;
import org.pojomatic.annotations.PojomaticPolicy;
import org.pojomatic.annotations.Property;

import com.google.common.collect.ImmutableList;

@AutoProperty(autoDetect=AutoDetectPolicy.NONE)
public class Seq extends Node implements Iterable<Node> {
	private final List<Node> nodes;
	private final Location end;
	
	private Seq(final Location location, final Location end, final List<Node> nodes) {
		super(location);
		this.end = end;
		this.nodes = ImmutableList.copyOf(nodes);
	}

	public Node get(final int arg0) {
		return nodes.get(arg0);
	}

	public boolean isEmpty() {
		return nodes.isEmpty();
	}

	@Override
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
	public void accept(final ISExpressionVisitor visitor) {
		super.accept(visitor);
		visitor.open();
		for (final Node node : this) {
			node.accept(visitor);
		}
		visitor.locate(end);
		visitor.close();
	}
	
	public Location getEndLocation() {
		return end;
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
		
		private Builder(final Location start) {
			super();
			this.start = start;
		}

		public Builder add(final Node node) {
			builder.add(node);
			return this;
		}
		
		public Builder add(final String atom) {
			return add(Atom.create(atom));
		}
		
		public Builder add(final String atom, final Node value) {
			add(atom + ":");
			return add(value);
		}
		
		public Builder add(final String atom, final String value) {
			add(atom+":");
			return add(value);
		}
		
		public Seq build(final Location end) {
			return new Seq(start, end, this.builder.build());
		}
		
		@Override
		public String toString() {
			return ""+builder.build();
		}

		public Builder addAll(final Collection<Node> nodes) {
			builder.addAll(nodes);
			return this;
		}
	}
	
	public static Builder builder(final Location start) {
		return new Builder(start);
	}
	
	@Property(policy=PojomaticPolicy.HASHCODE_EQUALS)
	public List<Node> getNodes() {
		return nodes;
	}
	
	@Override
	public void accept(final INodeVisitor visitor) {
		if (visitor.seq(this)) {
			for (final Node n : nodes) n.accept(visitor);
		}
	}
	
	@Override
	public boolean equals(final Object obj) {
		return Pojomatic.equals(this, obj);
	}
	
	@Override
	public int hashCode() {
		return Pojomatic.hashCode(this);
	}
}
