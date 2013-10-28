package com.larkery.jasb.sexp.parse;

import org.junit.Test;

import com.google.common.collect.ImmutableSet;
import com.larkery.jasb.sexp.ISexpSource;
import com.larkery.jasb.sexp.errors.IErrorHandler.IError;

public class ExpanderTest extends ParserTest {
	@Override
	protected ISexpSource source(String name, String src) {
		return Expander.expand(super.source(name, src), RECORD);
	}
	
	@Test
	public void templateGetsCutOut() {
		check("template cutout", "((template foo ()))", e("("), e(")"));
	}
	
	@Test
	public void templateGetsInserted() {
		check("template cutout", "((template foo () pling) (foo))", e("("), e("pling"), e(")"));
	}
	
	@Test
	public void templatesArgumentsAreSubstituted() {
		check("template cutout", "((template foo (@x) @x) (foo x:1))", e("("), e("1"), e(")"));
	}
	
	@Test
	public void templatesAreExpandedWithinTemplates() {
		check("template cutout", "((template foo (@x) @x) " + 
									"(template bar (@x) @x)"
				+"(foo x:(bar x:y)))", e("("), e("y"), e(")"));
	}
	
	@Test
	public void moreTemplatesAreExpandedInsideOtherExpansions() {
		check("template inside template", "((template foo (@x) @x) " + 
									"(template bar (@x) (@x))"
				+"(foo x:(bar x:y)))", e("("), e("("), e("y"), e(")"), e(")"));
	}
	
	@Test
	public void simplyRecursiveTemplatesCauseError() {
		check("single recursion", "((template foo () (foo)) (foo))", ImmutableSet.<Class<? extends IError>>of(IError.class));
	}
	
	@Test
	public void mutuallyRecursiveTemplatesCauseError() {
		check("mutual recursion", "((template foo () (bar)) (template bar () (foo)) (foo))", ImmutableSet.<Class<? extends IError>>of(IError.class));
	}
}
