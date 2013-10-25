package com.larkery.jasb.sexp;

import java.io.PrintStream;

public class PrintVisitor implements ISexpVisitor {
	private final PrintStream out;
	private Location location;
	private int depth = 1;
	private ISexpVisitor delegate;

	public PrintVisitor(PrintStream out) {
		super();
		this.out = out;
	}
	
	public PrintVisitor(PrintStream out, final ISexpVisitor delegate) {
		super();
		this.out = out;
		this.delegate = delegate;
	}

	@Override
	public void locate(Location loc) {
		if (delegate != null) delegate.locate(loc);
		this.location = loc;
	}

	@Override
	public void open() {
		tabs();
		out.println('(');
		depth++;
		if (delegate != null) delegate.open();
	}

	private void tabs() {
		out.print(location);
		
		for (int i= 0; i<depth; i++) {
			out.print("\t");
		}
	}

	@Override
	public void atom(String string) {
		tabs();
		out.println(string);
		if (delegate != null) delegate.atom(string);
	}

	@Override
	public void close() {
		depth--;
		tabs();
		out.println(')');
		if (delegate != null) delegate.close();
	}
}