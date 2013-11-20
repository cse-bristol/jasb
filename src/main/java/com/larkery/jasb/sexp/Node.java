package com.larkery.jasb.sexp;



public abstract class Node implements ISExpression {
	private final Location location;
	
	protected Node(Location location) {
		super();
		this.location = location;
	}

	public Location getLocation() {
		return location;
	}
	
	@Override
	public void accept(ISExpressionVisitor visitor) {
		visitor.locate(location);
	}
	
	public static Node copy(final ISExpression source) {
		final NodeBuilder visitor = new NodeBuilder();
		source.accept(visitor);
		return visitor.get();
	}
	
	public abstract void accept(INodeVisitor visitor); 
}
