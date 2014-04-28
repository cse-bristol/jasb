package com.larkery.jasb.sexp.parse;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;
import com.larkery.jasb.sexp.Atom;
import com.larkery.jasb.sexp.Delim;
import com.larkery.jasb.sexp.ISExpression;
import com.larkery.jasb.sexp.Invocation;
import com.larkery.jasb.sexp.Node;
import com.larkery.jasb.sexp.NodeBuilder;
import com.larkery.jasb.sexp.Seq;
import com.larkery.jasb.sexp.errors.IErrorHandler;
import com.larkery.jasb.sexp.errors.JasbErrorException;
import com.larkery.jasb.sexp.errors.UnfinishedExpressionException;

public class TemplateTest extends VisitingTest {
	@Test
	public void templateCreationWorksForValidTemplate() throws Exception {
		final NodeBuilder nb = NodeBuilder.create();
		final List<IMacro> macros = Template.stripTemplates(source("templateCreation", "(top (template hello [@thing [@other-thing] [@third a]]))"),
															nb, 
															IErrorHandler.RAISE);

		Assert.assertEquals("one template should have been created", 1, macros.size());
		final IMacro mac = macros.get(0);

		Assert.assertEquals("thing, other-thing and third define all the args",
							ImmutableSet.of("thing", "other-thing", "third"),
							((Template)mac).getAllowedArgumentNames());

		Assert.assertEquals("thing is a required argument", 
							ImmutableSet.of("thing"),
							((Template)mac).getRequiredArgumentNames());

		final Node node = nb.getBestEffort();
		Assert.assertEquals("template is stripped out", Seq.builder(null, Delim.Paren).add(Atom.create("top")).build(null), node);
	}

	@Test
	public void expansionWorksAsExpected() throws Exception {
		final NodeBuilder nb = NodeBuilder.create();
		final List<IMacro> macros = Template.stripTemplates(source("templateCreation", 
																   "(top (template hello [@thing [@other-thing] [@third a]] (@thing @other-thing @third @third)))"),
															nb, 
															IErrorHandler.RAISE);
		
		final IMacro mac = macros.get(0);
		final ISExpression e = ((Template)mac).doTransform(Invocation.of(Node.copy(source("templateInput", "(hello thing:99)")), IErrorHandler.RAISE), null, IErrorHandler.RAISE);

		final Node node = Node.copy(e);

		Assert.assertEquals("Expanded node is what we were expecting", Node.copy(source("exemplar", "(99 a a)")), node);
	}

	@Test
	public void canExpandOneArgumentWithinAnother() throws Exception {
		final NodeBuilder nb = NodeBuilder.create();
		final List<IMacro> macros = Template.stripTemplates(source("templateCreation", 
																   "(top (template hello [@thing [@other-thing (+ 1 @thing)] [@third a]] (@thing @other-thing @third @third)))"),
															nb, 
															IErrorHandler.RAISE);
		
		final IMacro mac = macros.get(0);
		final ISExpression e = ((Template)mac).doTransform(Invocation.of(Node.copy(source("templateInput", "(hello thing:99)")), IErrorHandler.RAISE), null, IErrorHandler.RAISE);

		final Node node = Node.copy(e);

		Assert.assertEquals("Expanded node is what we were expecting", Node.copy(source("exemplar", "(99 (+ 1 99) a a)")), node);
	}
	
	// subsequent tests are all for errors

	@Test(expected=JasbErrorException.class)
	public void templateCreationFailsForUndeclaredArgument() throws Exception {
		final NodeBuilder nb = NodeBuilder.create();
		Template.stripTemplates(source("missingArgument", "(top (template hello [] @banana))"),
															nb, 
															IErrorHandler.RAISE);

	}
	
	@Test(expected=JasbErrorException.class)
	public void templateCreationFailsForDuplicateArgument() throws Exception {
		final NodeBuilder nb = NodeBuilder.create();
		Template.stripTemplates(source("missingArgument", "(top (template hello [@x @x] ))"),
															nb, 
															IErrorHandler.RAISE);

	}

	@Test(expected=JasbErrorException.class)
	public void templateCreationFailsForDuplicateArgumentAgain() throws Exception {
		final NodeBuilder nb = NodeBuilder.create();
		Template.stripTemplates(source("missingArgument", "(top (template hello [@x [@x]] ))"),
															nb, 
															IErrorHandler.RAISE);

	}
	
	@Test(expected=JasbErrorException.class)
	public void templateCreationFailsForMalformedArgumentName() throws Exception {
		final NodeBuilder nb = NodeBuilder.create();
		Template.stripTemplates(source("missingArgument", "(top (template hello [badarg] ))"),
															nb, 
															IErrorHandler.RAISE);

	}

	@Test(expected=JasbErrorException.class)
	public void templateCreationFailsForMalformedArgumentWithDefault() throws Exception {
		final NodeBuilder nb = NodeBuilder.create();
		Template.stripTemplates(source("missingArgument", "(top (template hello [(badarg)] ))"),
															nb, 
															IErrorHandler.RAISE);

	}

	@Test(expected=JasbErrorException.class)
	public void templateCreationFailsForMalformedArgumentWithDefault2() throws Exception {
		final NodeBuilder nb = NodeBuilder.create();
		Template.stripTemplates(source("missingArgument", "(top (template hello [[]] ))"),
															nb, 
															IErrorHandler.RAISE);

	}
	
	@Test(expected=JasbErrorException.class)
	public void templateCreationFailsIfUsingRestTwice() throws Exception {
		Template.stripTemplates(source("restTwice", "(template t [@rest @rest] )"), 
				NodeBuilder.create(), 
				IErrorHandler.RAISE);
	}
	
	@Test(expected=JasbErrorException.class)
	public void templateCreationFailsIfUsingRestInBodyOnly() throws Exception {
		Template.stripTemplates(source("missingRest", "(template t [] @rest)"), 
				NodeBuilder.create(), 
				IErrorHandler.RAISE);
	}
	
	@Test(expected=JasbErrorException.class)
	public void templateCreationFailsIfUsingNumberedArgInBodyonly() throws Exception {
		Template.stripTemplates(source("missingNumbered", "(template t [] @1)"), 
				NodeBuilder.create(), 
				IErrorHandler.RAISE);
	}
	
	@Test(expected=JasbErrorException.class)
	public void templateCreationFailsIfWronglyNumberedArg() throws Exception {
		Template.stripTemplates(source("wrongNumber", "(template t [@0])"), 
				NodeBuilder.create(), 
				IErrorHandler.RAISE);
	}
	
	@Test(expected=JasbErrorException.class)
	public void templateCreationFailsIfGapInNumberedArgs() throws Exception {
		Template.stripTemplates(source("numberingGap", "(template t [@1 @3])"), 
				NodeBuilder.create(), 
				IErrorHandler.RAISE);
	}
	
	@Test(expected=JasbErrorException.class)
	public void templateCreationFailsIfOptionalNumberedFollowedByMandatoryNumbered() throws Exception {
		Template.stripTemplates(source("numberingIllegalOptional", "(template t [[@1] @2])"), 
				NodeBuilder.create(), 
				IErrorHandler.RAISE);
	}
	
	@Test
	public void canUseRestArgs() throws UnfinishedExpressionException {
		final Template t = (Template) Template.stripTemplates(source("restArgs", "(template t [@rest] @rest)"), 
				NodeBuilder.create(), 
				IErrorHandler.RAISE).get(0);
		
		final ISExpression e = t.doTransform(Invocation.of(node("(t a b c)"), IErrorHandler.RAISE), null, IErrorHandler.RAISE);
		final Node node = Node.copy(e);
		
		Assert.assertEquals("Expanded node is what we were expecting", node("a b c"), node);

	}

	private Node node(final String source) throws UnfinishedExpressionException {
		return Node.copy(source("don't care", source));
	}
	
	@Test
	public void canUseNumberedArgs() throws UnfinishedExpressionException {
		final Template t = (Template) Template.stripTemplates(
				node("(template t [@1 @2] @2 @1)"), 
				NodeBuilder.create(), 
				IErrorHandler.RAISE).get(0);
		
		final ISExpression e = t.doTransform(Invocation.of(node("(t a b)"), IErrorHandler.RAISE), null, IErrorHandler.RAISE);
		final Node node = Node.copy(e);
		
		Assert.assertEquals("Expanded node is what we were expecting", node("b a"), node);
	}
	
	@Test
	public void canUseOptionalArguments() throws UnfinishedExpressionException {
		final Template t = (Template) Template.stripTemplates(
				node("(template t [[@a]] @a)"), 
				NodeBuilder.create(), 
				IErrorHandler.RAISE).get(0);
		
		final ISExpression e = t.doTransform(Invocation.of(node("(t a: thing)"), IErrorHandler.RAISE), null, IErrorHandler.RAISE);
		final Node node = Node.copy(e);
		
		Assert.assertEquals("Expanded node is what we were expecting", node("thing"), node);
	}
	
	@Test
	public void canUseOptionalNumberedArguments() throws UnfinishedExpressionException {
		final Template t = (Template) Template.stripTemplates(
				node("(template t [[@1]] @1)"), 
				NodeBuilder.create(), 
				IErrorHandler.RAISE).get(0);
		
		final ISExpression e = t.doTransform(Invocation.of(node("(t thing)"), IErrorHandler.RAISE), null, IErrorHandler.RAISE);
		final Node node = Node.copy(e);
		
		Assert.assertEquals("Expanded node is what we were expecting", node("thing"), node);
	}
}
