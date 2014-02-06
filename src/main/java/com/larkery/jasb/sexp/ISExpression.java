package com.larkery.jasb.sexp;

public interface ISExpression {
	final ISExpression EMPTY = new ISExpression(){
		@Override
		public void accept(final ISExpressionVisitor visitor) {}
	};

	public void accept(ISExpressionVisitor visitor);
}
