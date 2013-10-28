package com.larkery.jasb.bind.id;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.reflect.TypeToken;
import com.google.common.util.concurrent.FutureCallback;
import com.larkery.jasb.sexp.Atom;

public class Resolver implements IResolver {
	static class Definition<Q> {
		public final String id;
		public final TypeToken<Q> type;
		public final Q value;
		public Definition(String id, TypeToken<Q> type, Q value) {
			super();
			this.id = id;
			this.type = type;
			this.value = value;
		}
	}
	
	static class Requirement<Q> {
		public final Atom cause;
		public final String id;
		public final TypeToken<Q> type;
		public final FutureCallback<Q> callback;
		public Requirement(final Atom cause, String id, TypeToken<Q> type,
				FutureCallback<Q> callback) {
			super();
			this.cause = cause;
			this.id = id;
			this.type = type;
			this.callback = callback;
		}
	}
	
	class Block {
		private final Block parent;
		private final List<Block> children = new LinkedList<>();
		private final Set<TypeToken<?>> holds;
		private final Map<String, Definition<?>> definitions = new HashMap<>();
		private final Multimap<String, Requirement<?>> outstanding = ArrayListMultimap.create();
		
		public Block(final Set<Class<?>> holds) {
			final ImmutableSet.Builder<TypeToken<?>> b = ImmutableSet.builder();
			for (final Class<?> c : holds) {
				b.add(TypeToken.of(c));
			}
			this.holds = b.build();
			if (blockStack.isEmpty()) {
				this.parent = null;
			} else {
				this.parent = blockStack.peek();
				this.parent.children.add(this);
			}
		}
		
		public <Q> void resolve(final Requirement<Q> requirement) {
			final boolean resolved = tryResolving(requirement);
			if (!resolved) {
				outstanding.put(requirement.id, requirement);
			}
		}
		
		@SuppressWarnings("unchecked")
		private <Q> boolean tryResolving(final Requirement<Q> requirement) {
			if (definitions.containsKey(requirement.id)) {
				final Definition<?> def = definitions.get(requirement.id);
				if (requirement.type.isAssignableFrom(def.type)) {
					requirement.callback.onSuccess((Q) def.value);
					return true;
				}
			}
			
			if (parent != null) {
				return parent.tryResolving(requirement);
			} else {
				return false;
			}
		}
		
		public <Q> void define(final Definition<Q> definition) {
			boolean matched = false;
			for (final TypeToken<?> c : holds) {
				if (c.isAssignableFrom(definition.type)) {
					this.definitions.put(definition.id, definition);
					matched = true;
					break;
				}
			}
			
			if (matched) {
				retryResolving(definition.id);
			} else if (parent != null) {
				parent.define(definition);
			}
		}

		private void retryResolving(final String id) {
			final Iterator<Requirement<?>> iterator = outstanding.get(id).iterator();
			while (iterator.hasNext()) {
				if (tryResolving(iterator.next())) {
					iterator.remove();
				}
			}
			for (final Block child : children) {
				child.retryResolving(id);
			}
		}

		public void raiseExceptions() {
			for (final Requirement<?> unresolved : outstanding.values()) {
				unresolved.callback.onFailure(new UnresolvableIdentifierException(unresolved.cause));
			}
			for (final Block child : children) {
				child.raiseExceptions();
			}
		}
	}
	
	final Stack<Block> blockStack = new Stack<>();
	
	public Resolver() {
		
	}
	
	@Override
	public <Q> void resolve(
			final Atom cause,
			final String id, 
			final TypeToken<Q> type,
			final FutureCallback<Q> callback) {
		if (blockStack.isEmpty()) {
			callback.onFailure(new UnresolvableIdentifierException(cause));
		} else {
			blockStack.peek().resolve(new Requirement<>(cause, id, type, callback));
		}
	}

	@Override
	public void pushBlock(final Set<Class<?>> holds) {
		blockStack.push(new Block(holds));
	}

	@Override
	public void popBlock() {
		final Block pop = blockStack.pop();
		if (blockStack.isEmpty()) {
			pop.raiseExceptions();
		}
	}

	@Override
	public <Q> void define(
			final String id, 
			final TypeToken<Q> type, 
			final Q value) {
		if (!blockStack.isEmpty()) {
			blockStack.peek().define(new Definition<>(id, type, value));
		}
	}
}
