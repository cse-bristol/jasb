package com.larkery.jasb.sexp;



public abstract class Node implements ISexpSource {
	private final Location location;
	
	protected Node(Location location) {
		super();
		this.location = location;
	}

	public Location getLocation() {
		return location;
	}
	
	@Override
	public void accept(ISexpVisitor visitor) {
		visitor.locate(location);
	}
	
	public static Node copy(final ISexpSource source) {
		final NodeBuilder visitor = new NodeBuilder();
		source.accept(visitor);
		return visitor.get();
	}
	
	public abstract void accept(INodeVisitor visitor); 
}
