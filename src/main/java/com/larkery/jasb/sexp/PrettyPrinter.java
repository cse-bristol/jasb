package com.larkery.jasb.sexp;

import java.io.PrintWriter;
import java.net.URI;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.base.Function;
import com.larkery.jasb.sexp.errors.IErrorHandler;


public class PrettyPrinter implements INodeVisitor {
	private final Map<URI, IndentedWriter> writers = new HashMap<>();
	private final LinkedList<URI> locations = new LinkedList<>();
	private final Function<URI, PrintWriter> writerSource;
	private IndentedWriter iw;
	
	public PrettyPrinter(final Function<URI, PrintWriter> writerSource) {
		this.writerSource = writerSource;
	}
	
	private IndentedWriter writer() {
		return writers.get(locations.peek());
	}
	
	private void switchInclude(final Node node) {
		final Location loc = node.getLocation();
		if (loc == null) {
			switchInclude(URI.create("nowhere:nowhere"));
		} else {
			switchInclude(loc.name);
		}
		
		this.iw = writer();
		if (this.iw == null) throw new UnsupportedOperationException("null");
	}
	
	private void switchInclude(final URI name) {
		if (!name.equals(locations.peek())) {
			if (locations.contains(name)) {
				while (!name.equals(locations.pop()));
			} else {
				locations.push(name);
				if (!writers.containsKey(name)) {
					writers.put(name, new IndentedWriter(writerSource.apply(name)));
				}
			}
		}
	}

	@Override
	public boolean seq(final Seq seq) {
		switchInclude(seq);
		
		final Invocation inv = Invocation.of(seq, IErrorHandler.SLF4J);
		if (inv != null) {
			if (iw.isAtSpace() == false) iw.write(" ");
			iw.write("(" + inv.name);
			iw.pushIndentation(inv.name.length() + 2);
			
			if (inv.arguments.size() == 1 && inv.remainder.isEmpty()) {
				final Entry<String, Node> next = inv.arguments.entrySet().iterator().next();
				iw.write(" " + next.getKey() + ": ");
				iw.pushIndentation(next.getKey().length() + 3);
				next.getValue().accept(this);
				iw.popIndentation();
			} else if (inv.arguments.isEmpty() && inv.remainder.size() == 1) {
				inv.remainder.iterator().next().accept(this);
			} else {
				for (final Map.Entry<String, Node> e : inv.arguments.entrySet()) {
					iw.write((iw.isAtSpace() ? "" : " ") + e.getKey() + ": ");
					iw.pushIndentation(e.getKey().length()+2);
					e.getValue().accept(this);
					iw.popIndentation();
					iw.write("\n");
				}
				
				for (final Node n : inv.remainder) {
					iw.write("\n");
					n.accept(this);
				}
			}
			iw.popIndentation();
			iw.write(")\n");
		} else {
			iw.pushIndentation(3);
			
			iw.write("(");
			
			for (final Node n : seq) {
				n.accept(this);
			}
			
			iw.write(")");
			
			iw.popIndentation();
		}
		
		return false;
	}

	@Override
	public void atom(final Atom atom) {
		switchInclude(atom);
		
			if (iw.getColumn() > 1000) {
				iw.write("\n");
			} else if (iw.isAtSpace() == false) {
				iw.write(" ");
			}
			iw.write(atom.toString());
		
	}

	@Override
	public void comment(final Comment comment) {
		switchInclude(comment);
		
		iw.write("\n ;; " + comment.getText() + "\n");
	}
}
