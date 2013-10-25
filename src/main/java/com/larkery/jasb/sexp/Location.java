package com.larkery.jasb.sexp;

public class Location {
	private final String name;
	public final long offset;
	public final long line;
	public final long column;
	
	private Location(String name, long offset, long line, long column) {
		super();
		this.name = name;
		this.offset = offset;
		this.line = line;
		this.column = column;
	}
	
	public static Location of(String locationName, long offset, long line, long column) {
		return new Location(locationName, offset, line, column);
	}

	@Override
	public String toString() {
		return String.format("%s:%s:%s", name, line, column);
	}
}
