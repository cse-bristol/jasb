package com.larkery.jasb.sexp;

public class Location {
	public enum Type {
		Normal,
		Include,
		Template
	}
	
	public final String name;
	public final long offset;
	public final long line;
	public final long column;
	public final Type type;
	
	private Location(final Type type, final String name, final long offset, final long line, final long column) {
		super();
		this.type = type;
		this.name = name;
		this.offset = offset;
		this.line = line;
		this.column = column;
	}
	
	public static Location of(final Type type, final String locationName, final long offset, final long line, final long column) {
		return new Location(type, locationName, offset, line, column);
	}

	@Override
	public String toString() {
		return String.format("%s:%s:%s", name, line, column);
	}
}
