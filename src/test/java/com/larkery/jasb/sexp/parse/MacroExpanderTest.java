package com.larkery.jasb.sexp.parse;

import com.larkery.jasb.sexp.NodeBuilder;
import java.util.List;
import com.larkery.jasb.sexp.Node;
import com.larkery.jasb.sexp.errors.IErrorHandler;

import org.junit.Test;
import com.larkery.jasb.sexp.Seq;
import com.larkery.jasb.sexp.Invocation;
import com.larkery.jasb.sexp.Delim;
import java.util.Collections;
import com.google.common.collect.ImmutableList;
import java.util.Set;
import com.larkery.jasb.sexp.ISExpression;
import com.larkery.jasb.sexp.ISExpressionVisitor;

public class MacroExpanderTest extends VisitingTest {
	@Test
	public void macroExpanderExpandsMacros() throws Exception {
		final NodeBuilder nb = NodeBuilder.create();
		final List<IMacro> macs = Template.stripTemplates(source("macExTest", 
																 "(top (template test [@a [@b]] (@a @b @a)) (test a:(this is some a)))"),
														  nb, IErrorHandler.RAISE);

		final Node n = Node.copy(MacroExpander.expand(macs, nb.get(), IErrorHandler.RAISE));
	}

	@Test
	public void macroExpanderExpandsArgumentsToOtherMacros() throws Exception {
		final NodeBuilder nb = NodeBuilder.create();
		final List<IMacro> macs = Template.stripTemplates(source("macExTest", 
																 "(top (template test [@a [@b]] (@a @b @a)) (test (test-macro)))"),
														  nb, IErrorHandler.RAISE);

		final Node n = nb.get();

		final Node n2 = Node.copy(MacroExpander.expand(ImmutableList.<IMacro>builder()
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

		final Node n2 = Node.copy(MacroExpander.expand(ImmutableList.<IMacro>builder()
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
				public void accept(final ISExpressionVisitor v) {
					v.atom("a:");
					v.atom("b");
				}
			};
		}
	}
}
