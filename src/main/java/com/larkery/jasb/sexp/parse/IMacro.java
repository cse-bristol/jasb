package com.larkery.jasb.sexp.parse;

import java.util.Set;
import com.larkery.jasb.sexp.ISExpression;
import com.larkery.jasb.sexp.Invocation;
import com.larkery.jasb.sexp.errors.IErrorHandler;

import com.larkery.jasb.sexp.ISExpression;
import com.larkery.jasb.sexp.Invocation;
import com.larkery.jasb.sexp.errors.IErrorHandler;

/**
 * Implementors can be by {@link Expander} to do source transformations on {@link Node}s. The expander will process each
 * invocation it sees; if it sees an invocation which matches a macro registered with it, it will use the transform
 * method to apply the macro and expand the invocation.
 *
 * An invocation matches if it has a matching name and pattern of arguments.
 */
public interface IMacro {
	/**
	 * The name which any invocation of this macro must have
	 */
	public String getName();

	/**
	 * Used to indicate what keyword arguments are required for this template to succeed
	 */
	public Set<String> getRequiredArgumentNames();

	/**
	 * Used to indicate what keyword arguments are acceptable for this template to succeed
	 */
	public Set<String> getAllowedArgumentNames();

	/**
	 * Used to indicate the maximum number of non-keyword arguments allowed
	 */
	public int getMaximumArgumentCount();

	/**
	 * Used to indicate the minimum number of non-keyword arguments allowed.
	 */
	public int getMinimumArgumentCount();

	/**
	 * Invoked to rewrite an {@link Invocation}
	 */
	public ISExpression transform(final Invocation expanded, final IErrorHandler errors);
}
