package com.larkery.jasb.sexp.parse;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.larkery.jasb.sexp.ISExpression;
import com.larkery.jasb.sexp.ISExpressionVisitor;
import com.larkery.jasb.sexp.Invocation;
import com.larkery.jasb.sexp.Node;
import com.larkery.jasb.sexp.NodeBuilder;
import com.larkery.jasb.sexp.errors.IErrorHandler;

public class MacroExpanderTest extends VisitingTest {
	@Test
	public void macroExpanderExpandsMacros() throws Exception {
		final NodeBuilder nb = NodeBuilder.create();
		final List<IMacro> macs = Template.stripTemplates(source("macExTest", 
																 "(top (template test [@a [@b]] (@a @b @a)) (test a:(this is some a)))"),
														  nb, IErrorHandler.RAISE);

		Node.copy(MacroExpander.expand(macs, nb.get(), IErrorHandler.RAISE));
	}

	@Test
	public void macroExpanderExpandsArgumentsToOtherMacros() throws Exception {
		final NodeBuilder nb = NodeBuilder.create();
		final List<IMacro> macs = Template.stripTemplates(source("macExTest", 
																 "(top (template test [@a [@b]] (@a @b @a)) (test (test-macro)))"),
														  nb, IErrorHandler.RAISE);

		final Node n = nb.get();

		Node.copy(MacroExpander.expand(ImmutableList.<IMacro>builder()
													  .addAll(macs)
													  .add(new TestMacro())
													  .build(), 
													   n, 
													   IErrorHandler.RAISE));
	}

	@Test
	public void macroExpanderExpandsArgumentsToOtherMacros2() throws Exception {
		final NodeBuilder nb = NodeBuilder.create();
		final List<IMacro> macs = Template.stripTemplates(source("macExTest", 
																 "(top (template a-is-b [] a: b) (template test [@a [@b]] (@a @b @a)) (test (a-is-b)))"),
														  nb, IErrorHandler.RAISE);

		final Node n = nb.get();

		Node.copy(MacroExpander.expand(ImmutableList.<IMacro>builder()
													  .addAll(macs)
													  .add(new TestMacro())
													  .build(), 
													   n, 
													   IErrorHandler.RAISE));
	}

	static class TestMacro 	implements IMacro {
		@Override
		public String getName() {
			return "test-macro";
		}

		@Override
		public Set<String> getRequiredArgumentNames() {
			return Collections.emptySet();
		}

		@Override
		public Set<String> getAllowedArgumentNames() {
			return Collections.emptySet();
		}

		@Override
		public int getMaximumArgumentCount() {
			return 0;
		}

		@Override
		public int getMinimumArgumentCount() {
			return 0;
		}

		@Override
		public ISExpression transform(final Invocation expanded, final IErrorHandler errors) {
			return new ISExpression() {
				@Override
				public void accept(final ISExpressionVisitor v) {
					v.atom("a:");
					v.atom("b");
				}
			};
		}
	}
}
