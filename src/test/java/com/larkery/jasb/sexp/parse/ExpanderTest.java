package com.larkery.jasb.sexp.parse;

import org.junit.Test;

import com.google.common.collect.ImmutableSet;
import com.larkery.jasb.sexp.ISExpression;
import com.larkery.jasb.sexp.errors.IErrorHandler.IError;

public class ExpanderTest extends ParserTest {
	@Override
	protected ISExpression source(final String name, final String src) {
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
	public void templateGetsInsertedDoubledUp() {
		check("template cutout", "((template foo (@a) @a @a) (foo a:x))", e("("), e("x"), e("x"), e(")"));
	}
	
	@Test
	public void templatesArgumentsAreSubstitutedAndOverrideDefaults() {
		check("template cutout", "((template foo (@x 2) @x) (foo x:1))", e("("), e("1"), e(")"));
	}
	
	@Test
	public void templatesAreExpandedWithinTemplates() {
		check("template cutout", "((template foo (@x) @x) " + 
									"(template bar (@x) @x)"
				+"(foo x:(bar x:y)))", e("("), e("y"), e(")"));
	}
	
	@Test
	public void templatesAreExpandedWithinTemplatesEvenIfTheirDefinitionsAreInOtherTemplates() {
		check("template cutout", 
				"("
				+ "(template outer (@a) @a)"
				+ "(outer a:(thing (widget) (template widget () other thing)"
				+ ")", 
				
				e("("), e("("), e("thing"), e("other"), e("thing"), e(")"), e(")"));
	}
	
	@Test
	public void templatesAreExpandedWithinTemplatesAgain() {
		check("template cutout", "((template foo (@x) @x) " + 
									"(template bar (@another) @another)"
				+"(foo x:(bar another:y)))", e("("), e("y"), e(")"));
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
	
	@Test
	public void defaultValuesAreSubstituted() {
		check("template cutout", "((template foo (@x 1) @x) (foo))", e("("), e("1"), e(")"));
	}
	
	@Test
	public void missingTemplateArgumentMakesError() {
		check("no arg", "((template foo (@widgets) (bar)) (foo) )", ImmutableSet.<Class<? extends IError>>of(IError.class));
	}
	
	@Test
	public void unexpectedTemplateArgumentMakesError() {
		check("extra arg", "((template foo (@widgets) (bar)) (foo widgets:1 sprockets:2) )", ImmutableSet.<Class<? extends IError>>of(IError.class));
	}
}
