package com.larkery.jasb.sexp;

import java.io.PrintStream;

public class PrintVisitor implements ISExpressionVisitor {
	private final PrintStream out;
	private Location location;
	private int depth = 1;
	private ISExpressionVisitor delegate;

	public PrintVisitor(final PrintStream out) {
		super();
		this.out = out;
	}
	
	public PrintVisitor(final PrintStream out, final ISExpressionVisitor delegate) {
		super();
		this.out = out;
		this.delegate = delegate;
	}

	@Override
	public void locate(final Location loc) {
		if (delegate != null) delegate.locate(loc);
		this.location = loc;
	}

	@Override
	public void open(final Delim delimeter) {
		tabs();
		out.println(delimeter);
		depth++;
		if (delegate != null) delegate.open(delimeter);
	}

	private void tabs() {
		out.print(location);
		
		for (int i= 0; i<depth; i++) {
			out.print("\t");
		}
	}

	@Override
	public void atom(final String string) {
		tabs();
		out.println(string);
		if (delegate != null) delegate.atom(string);
	}
	
	@Override
	public void comment(final String text) {
		tabs();
		out.println(";;" + text);
		if (delegate != null) delegate.comment(text);
	}

	@Override
	public void close(final Delim delimeter) {
		depth--;
		tabs();
		out.println(')');
		if (delegate != null) delegate.close(delimeter);
	}
}
