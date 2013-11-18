package com.larkery.jasb.sexp;

import java.io.PrintWriter;
import java.net.URI;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;

import com.larkery.jasb.sexp.errors.IErrorHandler;


public class PrettyPrinter implements INodeVisitor {
	private final IndentedWriter iw;
	private final boolean makeIncludes;
	private final LinkedList<String> locations = new LinkedList<>();
	
	public PrettyPrinter(final PrintWriter out, final boolean makeIncludes) {
		this.makeIncludes = makeIncludes;
		iw = new IndentedWriter(out);
	}
	
	private boolean shouldPrint(final Node atom) {
		if (!makeIncludes) return true;
		
		final Location loc = atom.getLocation();
		if (loc == null) return true;
		
		if (!loc.name.equals(locations.peek())) {
			System.err.println(loc.name + " not current location");
			if (locations.contains(loc.name)) {
				System.err.println("popping back to it");
				// pop up to that place (presuming no recursion)
				while (!loc.name.equals(locations.pop()));
			} else {
				System.err.println("pushing it");
				locations.push(loc.name);
				if (locations.size() == 2) {
					final URI top = URI.create(locations.get(0));
					final URI bottom = URI.create(locations.get(1));
					
					iw.write(String.format("(include href:\"%s\")", bottom));
				}
			}
		}
		
		return locations.size() <= 1;
	}
	
	@Override
	public boolean seq(final Seq seq) {
		if (shouldPrint(seq)) {
			final Invocation inv = Invocation.of(seq, IErrorHandler.SLF4J);
			if (inv != null) {
				iw.write("(" + inv.name);
				iw.pushIndentation(inv.name.length() + 2);
				
				if (inv.arguments.size() == 1 && inv.remainder.isEmpty()) {
					final Entry<String, Node> next = inv.arguments.entrySet().iterator().next();
					iw.write(" " + next.getKey() + ": ");
					iw.pushIndentation(next.getKey().length() + 3);
					next.getValue().accept(this);
					iw.popIndentation();
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
		}
		return false;
	}

	@Override
	public void atom(final Atom atom) {
		if (shouldPrint(atom)) {
			if (iw.getColumn() > 1000) {
				iw.write("\n");
			} else if (iw.isAtSpace() == false) {
				iw.write(" ");
			}
			iw.write(atom.toString());
		}
	}

	@Override
	public void comment(final Comment comment) {
		if (shouldPrint(comment)) {
			iw.write("\n ;; " + comment.getText() + "\n");
		}
	}
}
