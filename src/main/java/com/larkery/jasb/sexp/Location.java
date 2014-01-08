package com.larkery.jasb.sexp;

import java.net.URI;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;

/**
 * Represents a location for an event in an s-expression event stream,
 * or a node in an actual expression. Since an event may come from an
 * include, or a template expansion, the location can be in several
 * places.
 */
public class Location {
	public enum Type {
		Normal,
		Include,
		Template
	}
	
	public final List<Position> positions;
	
	public static class Position {
		public final URI name;
		public final int line;
		public final int column;
		public final Type type;
	
		private Position(final Type type, final URI name, final int line, final int column) {
			this.type = type;
			this.name = name;
			this.line = line;
			this.column = column;
		}

		public static Position of(final URI name, final int line, final int column) {
			return of(Type.Normal, name, line, column);
		}
		
		public static Position of(final Type type, final URI name, final int line, final int column) {
			return new Position(type, name, line, column);
		}

		@Override
		public String toString() {
			return String.format("%s:%s:%s", name, line, column);
		}
		
		public Position withType(final Type type) {
			return of(type, this.name, this.line, this.column);
		}
	}

	private Location(final List<Position> where) {
		super();
		this.positions = ImmutableList.copyOf(where);
	}
	
	public static Location of(final URI name, final int line, final int column) {
		return of(ImmutableList.of(Position.of(name, line, column)));
	}
	
	public static Location of(final List<Position> positions) {
		return new Location(ImmutableList.copyOf(positions));
	}

	public Location withTypeOfTail(final Type type) {
		final Builder<Position> builder = ImmutableList.<Position>builder();
		for (int i = 0; i<positions.size()-1; i++) {
			builder.add(positions.get(i));
		}
		
		builder.add(positions.get(positions.size()-1).withType(type));
		return of(builder.build());
	}
	
	public Location withTypeAndPosition(final Type type, final Position inner) {
		return of(ImmutableList.<Position>builder().addAll(withTypeOfTail(type).positions).add(inner).build());
	}

	public final Position getSourcePosition() {
		if (positions.isEmpty()) return null;
		return positions.get(0);
	}

	@Override
	public String toString() {
		return positions.toString();
	}

	public Location appending(final URI name, final int line, final int column) {
		return of(ImmutableList.<Position>builder().addAll(positions).add(Position.of(name, line, column)).build());
	}

	public Location appending(final Location loc) {
		return of(ImmutableList.<Position>builder().addAll(positions).addAll(loc.positions).build());
	}
}
