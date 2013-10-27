package com.larkery.jasb.bind;

import java.util.IdentityHashMap;
import java.util.Map;

import com.larkery.jasb.sexp.Node;

public class BindingTree<A> {
	private final A root;
	private final Map<Object, Object> containments = new IdentityHashMap<>();
	private final Map<Object, Node> nodesByObject = new IdentityHashMap<>();
	private final Map<Node, Object> objectsByNode = new IdentityHashMap<>();
	
	public BindingTree(A root) {
		super();
		this.root = root;
	}
	
	public A getRoot() {
		return root;
	}
	
	
}
