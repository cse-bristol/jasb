package com.larkery.jasb.sexp;

import java.util.Map;

import com.larkery.jasb.sexp.errors.IErrorHandler;

public class PrettyPrinter implements INodeVisitor {
	int indentation = 0;
	private void print(final String string) {
		
	}
	
	@Override
	public void seq(Seq seq) {
		final Invocation invocation = Invocation.of(seq, IErrorHandler.SLF4J);
		if (invocation == null) {
			
		} else {
			print(String.format("(%s", invocation.name));
			indentation++;
			for (final Map.Entry<String, Node> e : invocation.arguments.entrySet()) {
				
			}
			indentation--;
			print(")");
		}
	}


	@Override
	public void atom(final Atom atom) {
		
	}

	@Override
	public void comment(final Comment comment) {

	}

}
