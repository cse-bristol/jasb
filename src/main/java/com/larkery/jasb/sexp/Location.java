package com.larkery.jasb.sexp;

import java.net.URI;
import java.util.List;
import com.google.common.collect.ImmutableList;

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
	
	public final Type type;
	public final List<Position> positions;
	
	public static class Position {
		public final URI name;
		public final int line;
		public final int column;
	
		private Position(final URI name, final int line, final int column) {
			this.name = name;
			this.line = line;
			this.column = column;
		}

		public static Position of(final URI name, final int line, final int column) {
			return new Position(name, line, column);
		}

		@Override
		public String toString() {
			return String.format("%s:%s:%s", name, line, column);
		}
	}

	private Location(final Type type, final List<Position> where) {
		super();
		this.type = type;
		this.positions = ImmutableList.copyOf(where);
	}
	
	public static Location of(final Type type, final List<Position> positions) {
		return new Location(type, ImmutableList.copyOf(positions));
	}

	@Override
	public String toString() {
		return positions.toString();
	}
}
