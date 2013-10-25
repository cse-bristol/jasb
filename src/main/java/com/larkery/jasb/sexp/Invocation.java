package com.larkery.jasb.sexp;

import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class Invocation {
	public final String name;
	public final Map<String, Node> arguments;
	public final List<Node> remainder;
	
	private Invocation(String name, Map<String, Node> arguments,
			List<Node> remainder) {
		super();
		this.name = name;
		this.arguments = arguments;
		this.remainder = remainder;
	}
	
	public static final Invocation of(final Node node, final IErrorHandler errors) {
		if (node instanceof Seq) {
			final Seq seq = (Seq) node;
			if (seq.size() == 0) {
				errors.error(node.getLocation(), "an empty list was not expected here");
			} else {
				final Node head = seq.getHead();
				
				if (head instanceof Atom) {
					final Atom name = (Atom) head;
					final List<Node> tail = seq.getTail();
					final HashSet<String> seenArguments = new HashSet<String>();
					final ImmutableMap.Builder<String, Node> arguments = ImmutableMap.builder();
					final ImmutableList.Builder<Node> rest = ImmutableList.builder();
					boolean inRest = false;
					String key = null;
					for (final Node argument : tail) {
						final String thisKey;
						if (argument instanceof Atom) {
							final Atom a = (Atom) argument;
							if (a.getValue().endsWith(":")) {
								thisKey = a.getValue().substring(0, a.getValue().length()-1);
							} else {
								thisKey = null;
							}
						} else {
							thisKey = null;
						}
						
						if (key == null && thisKey != null) {
							if (inRest) {
								errors.error(argument.getLocation(), "unexpected keyword " + argument);
								return null;
							} else {
								key = thisKey;
							}
						} else if (key != null) {
							if (seenArguments.contains(key)) {
								errors.error(argument.getLocation(), "repeated keyboard " + key);
								return null;
							} else {
								arguments.put(key, argument);
							}
							key = null;
						} else {
							inRest = true;
							rest.add(argument);
						}
					}
					
					return new Invocation(name.getValue(), arguments.build(), rest.build());
				} else {
					errors.error(head.getLocation(), "a word was expected here, not a list");
				}
				
			}
		} else {
			errors.error(node.getLocation(), "a list was expected here, not a word");
		}
		return null;
	}
}