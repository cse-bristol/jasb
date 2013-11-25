package com.larkery.jasb.sexp;


public abstract class Node implements ISExpression {
	private final Location location;
	
	protected Node(final Location location) {
		super();
		this.location = location;
	}

	public Location getLocation() {
		return location;
	}
	
	@Override
	public void accept(final ISExpressionVisitor visitor) {
		visitor.locate(location);
	}
	
	public static Node copyStructure(final ISExpression source) {
		final NodeBuilder visitor = NodeBuilder.withoutComments();
		source.accept(visitor);
		return visitor.get();
	}
	
	public static Node copy(final ISExpression source) {
		final NodeBuilder visitor = NodeBuilder.create();
		source.accept(visitor);
		return visitor.get();
	}
	
	public abstract void accept(INodeVisitor visitor); 
}
