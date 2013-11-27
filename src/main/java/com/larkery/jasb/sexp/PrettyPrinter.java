package com.larkery.jasb.sexp;

import java.util.Stack;

import com.google.common.base.CharMatcher;


public class PrettyPrinter implements ISExpressionVisitor {
	int maxWidth = 100;
	String tabs = "  ";
	Stack<IndentedBuffer> stack = new Stack<>();
	
	class IndentedBuffer {
		private final StringBuffer theBuffer = new StringBuffer();
		private final int indentation;
		private int lineWidth = 0;
		private int kwShift = 0;
		
		IndentedBuffer(final int indentation) {
			super();
			this.indentation = indentation;
		}
		
		public void append(String string) {
			// append with wrapping
			if (!string.equals(")") && kwShift == 2) {
				theBuffer.append("\n");
				lineWidth = tabs.length() * (indentation);
			}
			if (lineWidth + string.length() > maxWidth) {
				theBuffer.append(tabs);
				theBuffer.append("\n");
				lineWidth = string.length() + (tabs.length() * (indentation+1));
				theBuffer.append(string);
			} else {
				lineWidth += string.length();
				if (!string.equals(")") && pad()) {
					string = " " + string;
				}
				theBuffer.append(string);
			}
			if (string.endsWith(":")) {
				kwShift = 1;
			} else if (kwShift == 1) {
				kwShift = 2;
			} else {
				kwShift = 0;
			}
		}

		private boolean pad() {
			if (theBuffer.length() == 0) return false;
			final char end = theBuffer.charAt(theBuffer.length()-1);
			if (end == '(') return false;
			if (end == ')') return false;
			if (CharMatcher.WHITESPACE.matches(end)) return false;
			return true;
		}

		public void appendNL(final String string) {
			append(string);
			theBuffer.append("\n");
			theBuffer.append(tabs);
			lineWidth = tabs.length() * (indentation+1);
		}
		
		@Override
		public String toString() {
			final String manyTabs = new String(new char[tabs.length() * indentation]).replace("\0", tabs);
			return theBuffer.toString().replace("\n", "\n"+manyTabs);
		}
	}
	
	public PrettyPrinter() {
		stack.push(new IndentedBuffer(0));
	}
	
	@Override
	public void locate(final Location loc) {
		
	}

	@Override
	public void open() {
		final IndentedBuffer buffer = new IndentedBuffer(stack.size());
		buffer.append("(");
		stack.push(buffer);
	}

	@Override
	public void atom(String string) {
		if (CharMatcher.WHITESPACE.matchesAnyOf(string)) {
			string = "\"" + string.replace("\"", "\\\"") + "\"";
		}
		stack.peek().append(string);
	}

	@Override
	public void comment(final String text) {
		stack.peek().appendNL("; " + text);
		
	}

	@Override
	public void close() {
		final IndentedBuffer pop = stack.pop();
		pop.append(")");
		stack.peek().append(pop.toString());
	}
	
	@Override
	public String toString() {
		return stack.peek().toString();
	}
	
	public static String toString(final ISExpression expression) {
		final PrettyPrinter pp = new PrettyPrinter();
		expression.accept(pp);
		return pp.toString();
	}
}
