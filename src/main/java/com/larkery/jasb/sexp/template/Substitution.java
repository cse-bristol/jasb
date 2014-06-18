 package com.larkery.jasb.sexp.template;

import java.util.Map;

import com.larkery.jasb.sexp.Delim;
import com.larkery.jasb.sexp.ISExpression;
import com.larkery.jasb.sexp.ISExpressionVisitor;
import com.larkery.jasb.sexp.Location;

class Substitution implements ISExpression {
	private final ISExpression body;
	private final Map<String, ISExpression> arguments;
	private final Location baseLocation;
	
	public Substitution(final Location baseLocation, final ISExpression body, final Map<String, ISExpression> arguments) {
		this.body = body;
		this.arguments = arguments;
		this.baseLocation = baseLocation.withTypeOfTail(Location.Type.Template);
	}

	@Override
	public void accept(final ISExpressionVisitor visitor) {
		final SubbingVisitor sv = new SubbingVisitor(visitor);
		body.accept(sv);
	}

	class SubbingVisitor implements ISExpressionVisitor {
		private final ISExpressionVisitor delegate;
		private boolean rewritingLocation = true;
		
		public SubbingVisitor(final ISExpressionVisitor delegate) {
			this.delegate = delegate;
		}

		@Override
		public void locate(final Location loc) {
			// rewrite location to give location of error within template usage
			// so that errors associate to the place the template is used, not where
			// it is defined
			if (rewritingLocation) {
				delegate.locate(baseLocation.appending(loc));
			}
		}

		@Override
		public void open(final Delim delimeter) {
			delegate.open(delimeter);
		}

		@Override
		public void atom(final String string) {
			if (arguments.containsKey(string)) {
				// this is a template parameter for this template,
				// so we want to put that in for where we are; its
				// source location is al ready OK so we don't need
				// to rewrite it.
				final ISExpression value = arguments.get(string);

				// disable location rewriting because we are visiting the argument and we want the error there
				rewritingLocation = false;
				value.accept(this);
				rewritingLocation = true;

			} else {
				delegate.atom(string);
			}
		}

		@Override
		public void comment(final String text) {
			delegate.comment(text);
		}

		@Override
		public void close(final Delim delimeter) {
			delegate.close(delimeter);
		}
	}
}