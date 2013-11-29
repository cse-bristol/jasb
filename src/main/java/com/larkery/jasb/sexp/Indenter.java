package com.larkery.jasb.sexp;

import java.io.StringWriter;
import java.net.URI;
import java.util.Deque;
import java.util.LinkedList;

import com.google.common.base.Joiner;

public class Indenter implements ISExpressionVisitor {
	final Deque<URI> uris = new LinkedList<>();
	final Deque<Integer> lines = new LinkedList<>();
	final Deque<String> tabs = new LinkedList<>();
	
	private int currentOutputLine, targetOutputLine;
	
	private final StringWriter result = new StringWriter();
	
	@Override
	public void locate(final Location here) {
		if (here == null) return;

		if (uris.isEmpty()) {
			uris.push(here.name);
			lines.push(here.line);
		} else {
			URI uri = uris.pop();
			int line = lines.pop();
			
			if (uri.equals(here.name)) {
				// we are in the same document, but we might have moved on a bit
				final int delta = here.line - line;
				targetOutputLine += Math.max(0, delta);
			} else if (uris.contains(here.name)) {
				do {
					line = lines.pop();
				} while (!(uri = uris.pop()).equals(here.name));
				final int delta = here.line - line;
				targetOutputLine += Math.max(0, delta);
			}
			
			uris.push(here.name);
			lines.push(here.line);
		}
	}

	void write(final String string) {
		final String tabin = Joiner.on("").join(tabs);
		while (currentOutputLine < targetOutputLine) {
			result.write("\n");
			result.write(tabin);
			currentOutputLine++;
		}
		result.write(string);
	}
	
	@Override
	public void open() {
		write("(");
		tabs.push("\t");
	}

	@Override
	public void atom(final String string) {
		write(string + " ");
	}

	@Override
	public void comment(final String text) {
		write("; " + text);
	}

	@Override
	public void close() {
		tabs.pop();
		write(")");
	}
	
	@Override
	public String toString() {
		return result.toString();
	}
	
	public static String toString(final ISExpression expression) {
		final Indenter i = new Indenter();
		expression.accept(i);
		return i.toString();
	}
}
