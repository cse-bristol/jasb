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
							mac.getAllowedArgumentNames());

		Assert.assertEquals("thing is a required argument", 
							ImmutableSet.of("thing"),
							mac.getRequiredArgumentNames());

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
		final ISExpression e = mac.transform(Invocation.of(Node.copy(source("templateInput", "(hello thing:99)")), IErrorHandler.RAISE), IErrorHandler.RAISE);

		final Node node = Node.copy(e);

		Assert.assertEquals("Expanded node is what we were expecting", Node.copy(source("exemplar", "(99 a a)")), node);
	}

	// subsequent tests are all for errors

	@Test(expected=JasbErrorException.class)
	public void templateCreationFailsForUndeclaredArgument() throws Exception {
		final NodeBuilder nb = NodeBuilder.create();
		final List<IMacro> macros = Template.stripTemplates(source("missingArgument", "(top (template hello [] @banana))"),
															nb, 
															IErrorHandler.RAISE);

	}
	
	@Test(expected=JasbErrorException.class)
	public void templateCreationFailsForDuplicateArgument() throws Exception {
		final NodeBuilder nb = NodeBuilder.create();
		final List<IMacro> macros = Template.stripTemplates(source("missingArgument", "(top (template hello [@x @x] ))"),
															nb, 
															IErrorHandler.RAISE);

	}

	@Test(expected=JasbErrorException.class)
	public void templateCreationFailsForDuplicateArgumentAgain() throws Exception {
		final NodeBuilder nb = NodeBuilder.create();
		final List<IMacro> macros = Template.stripTemplates(source("missingArgument", "(top (template hello [@x [@x]] ))"),
															nb, 
															IErrorHandler.RAISE);

	}
	
	@Test(expected=JasbErrorException.class)
	public void templateCreationFailsForMalformedArgumentName() throws Exception {
		final NodeBuilder nb = NodeBuilder.create();
		final List<IMacro> macros = Template.stripTemplates(source("missingArgument", "(top (template hello [badarg] ))"),
															nb, 
															IErrorHandler.RAISE);

	}

	@Test(expected=JasbErrorException.class)
	public void templateCreationFailsForMalformedArgumentWithDefault() throws Exception {
		final NodeBuilder nb = NodeBuilder.create();
		final List<IMacro> macros = Template.stripTemplates(source("missingArgument", "(top (template hello [(badarg)] ))"),
															nb, 
															IErrorHandler.RAISE);

	}

	@Test(expected=JasbErrorException.class)
	public void templateCreationFailsForMalformedArgumentWithDefault2() throws Exception {
		final NodeBuilder nb = NodeBuilder.create();
		final List<IMacro> macros = Template.stripTemplates(source("missingArgument", "(top (template hello [[]] ))"),
															nb, 
															IErrorHandler.RAISE);

	}
}
