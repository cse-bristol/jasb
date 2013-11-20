package com.larkery.jasb.sexp;

public interface ISExpressionVisitor {
	/**
	 * @param loc set the location for the next event
	 */
	public void locate(final Location loc);
	/**
	 * We saw a (
	 */
	public void open();
	/**
	 * We saw a word
	 * @param string the word
	 */
	public void atom(final String string);
	/**
	 * We saw a comment
	 * @param text the comment
	 */
	public void comment(final String text);
	/**
	 * We saw a )
	 */
	public void close();
}
