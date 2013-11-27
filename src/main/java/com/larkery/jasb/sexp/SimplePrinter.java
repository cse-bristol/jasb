package com.larkery.jasb.sexp;

public class SimplePrinter implements ISExpressionVisitor {
	private final StringBuffer buffer = new StringBuffer();
	
	public String getString() {
		return buffer.toString();
	}
	
	@Override
	public void locate(final Location loc) {
		
	}

	@Override
	public void open() {
		buffer.append('(');
	}

	@Override
	public void atom(final String string) {
		buffer.append(' ');
		buffer.append(string);
	}

	@Override
	public void comment(final String text) {
		
	}

	@Override
	public void close() {
		buffer.append(')');
		buffer.append('\n');
	}
	
	public static String toString(final ISExpression expression) {
		final SimplePrinter printer = new SimplePrinter();
		expression.accept(printer);
		return printer.getString();
	}
}
