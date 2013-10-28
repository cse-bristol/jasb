package com.larkery.jasb.sexp.parse;

import java.io.StringReader;

import org.junit.Test;

import com.larkery.jasb.sexp.IErrorHandler;
import com.larkery.jasb.sexp.PrintVisitor;

public class ExpanderTest {
	@Test
	public void testExpandSimple() {
		Expander.expand(Parser.source("test", new StringReader("(thing (template hello (@wang) @wang @wang) (hello wang:stuff))"), IErrorHandler.SLF4J), IErrorHandler.SLF4J)
			.accept(new PrintVisitor(System.out));
	}
	
	@Test
	public void testNoRecursionPlease() {
		Expander.expand(Parser.source("test", 
				new StringReader("(thing (template hello (@wang) (hello @wang)) (hello wang:stuff))"), IErrorHandler.SLF4J), IErrorHandler.SLF4J)
				.accept(new PrintVisitor(System.out));
	}
}
