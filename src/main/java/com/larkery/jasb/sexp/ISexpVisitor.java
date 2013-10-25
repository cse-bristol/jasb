package com.larkery.jasb.sexp;

public interface ISexpVisitor {
	public void locate(final Location loc);
	public void open();
	public void atom(final String string);
	public void close();
}
