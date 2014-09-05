package com.larkery.jasb.sexp;

import java.net.URI;

import com.google.common.base.Optional;
import com.larkery.jasb.sexp.errors.ILocated;

/**
 * Represents a location for an event in an s-expression event stream,
 * or a node in an actual expression. Since an event may come from an
 * include, or a template expansion, the location can be in several
 * places.
 */
public class Location implements ILocated {
	public static class Via {
		public enum Type {
			Normal,
			Include,
			Template
		}
		
		public final Type type;
		public final Location location;
		private Via(final Type type, final Location location) {
			super();
			this.type = type;
			this.location = location;
		}
	}
	
	public final URI name;
	public final int line;
	public final int column;
	public final Optional<Via> via;
	public final Location sourceLocation;
	
	private Location(final URI name, final int line, final int column, final Optional<Via> via) {
		super();
		this.name = name;
		this.line = line;
		this.column = column;
		this.via = via;
		if (this.via.isPresent()) {
			this.sourceLocation = this.via.get().location.sourceLocation;
		} else {
			this.sourceLocation = this;
		}
	}

	@Override
	public Location getLocation() {
		return this;
	}

	public static Location of(final URI uri, final int line2, final int column2, final Via.Type via, final Location sourceLocation) {
		return new Location(uri, line2, column2, Optional.of(new Via(via, sourceLocation)));
	}

	public static Location of(final URI uri, final int line2, final int column2) {
		return new Location(uri, line2, column2, Optional.<Via>absent());
	}

	public Location via(final Via.Type type, final Location baseLocation) {
		return of(name, line, column, type, baseLocation);
	}
	
	@Override
	public String toString() {
		if (via.isPresent()) {
			return String.format("%s:%s:%s (from %s %s)", name, line, column, via.get().type, via.get().location);
		} else {
			return String.format("%s:%s:%s", name, line, column);
		}
	}
}
