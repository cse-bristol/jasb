package com.larkery.jasb.io.impl;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.larkery.jasb.io.IReadContext;
import com.larkery.jasb.sexp.Atom;
import com.larkery.jasb.sexp.Invocation;
import com.larkery.jasb.sexp.Node;
import com.larkery.jasb.sexp.Seq;
import com.larkery.jasb.sexp.errors.UnexpectedTermError;
import com.larkery.jasb.sexp.errors.UnusedTermError;

/**
 * This is an abstract base class for things which can read invocations;
 * these are s-expression forms of the following sort:
 * 
 * (name [key: expr]* expr*)
 * 
 * <b>DO NOT</b> rename any of its methods, as they are used in generated subtypes
 * 
 * @author hinton
 *
 * @param <T>
 */
public abstract class InvocationReader<T> {
	protected final String name;
	protected final Set<String> expectedKeys;
	protected final Class<T> clazz;

	protected InvocationReader(final Class<T> clazz, final String name, final String[] expectedKeys) {
		this.clazz = clazz;
		this.name = name;
		this.expectedKeys = ImmutableSet.copyOf(expectedKeys);
	}

	/**
	 * This is overridden in subclasses to do the actual work
	 * @param context
	 * @param invocation
	 * @return
	 */
	protected abstract T read(final IReadContext context, final Invocation invocation);

	protected final void warnOnUnusedKeys(final IReadContext context, final Invocation invocation) {
		for (final Map.Entry<String, Node> entry : invocation.arguments.entrySet()) {
			if (expectedKeys.contains(entry.getKey())) continue;
			context.handle(new UnexpectedTermError(entry.getValue(), expectedKeys, entry.getKey()));
		}
	}
	
	protected static final void warnOnUnusedPositions(final IReadContext context, final List<Node> remainder, final int offset) {
		if (remainder.size() > offset) {
			context.handle(new UnusedTermError(ImmutableSet.copyOf(remainder.subList(offset, remainder.size()))));
		}
	}
	
	protected static final Node getNodeOrNull(final List<Node> nodes, final int index) {
		if (index >= nodes.size()) return null;
		return nodes.get(index);
	}
	
	protected static final <Q> ListenableFuture<List<Q>> readRemainder(final IReadContext context, final Class<Q> type, final List<Node> nodes, final int offset) {
		return context.readMany(type, nodes.subList(Math.min(offset, nodes.size()), nodes.size()));
	}
	
	protected final <Q> ListenableFuture<List<Q>> readOneOrMany(
			final IReadContext context,
			final Class<Q> type, 
			final Node node) {
		if (node instanceof Atom) {
			final Atom atom = (Atom) node;
			final ListenableFuture<Q> readValue = context.read(type, atom);
			return Futures.allAsList(ImmutableList.of(readValue));
		} else if (node instanceof Seq) {
			final Seq seq = (Seq) node;
			if (Invocation.isInvocation(seq)) {
				final ListenableFuture<Q> readValue = context.read(type, seq);
				return Futures.allAsList(ImmutableList.of(readValue));
			} else {
				return context.readMany(type, seq);
			}
		} else {
			throw new RuntimeException(node + " is neither a Seq nor an Atom, which should not happen");
		}
	}
	
	public String getName() {
		return name;
	}
}
