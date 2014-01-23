package com.larkery.jasb.sexp.parse;

import java.util.List;

import com.larkery.jasb.sexp.ISExpression;
import com.larkery.jasb.sexp.Node;
import com.larkery.jasb.sexp.NodeBuilder;
import com.larkery.jasb.sexp.errors.BasicError;
import com.larkery.jasb.sexp.errors.IErrorHandler;
import com.larkery.jasb.sexp.errors.UnfinishedExpressionException;

public class TemplateExpander {
	public static ISExpression expand(final ISExpression source, final IErrorHandler errors) {
		final NodeBuilder builder = NodeBuilder.create();
		
		final List<IMacro> templates = Template.stripTemplates(source, builder, errors);
		try {
			return MacroExpander.expand(templates, builder.get(), errors);
		} catch (final UnfinishedExpressionException uee) {
			final Node best = uee.getBestEffort();
			errors.handle(BasicError.at(uee.getUnclosed(), 
					"Unfinished expression encountered whilst extracting templates"));
			return best;
		}
	}
}
