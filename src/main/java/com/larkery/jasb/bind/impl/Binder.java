package com.larkery.jasb.bind.impl;

import java.util.Set;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.reflect.TypeToken;
import com.google.common.util.concurrent.FutureCallback;
import com.larkery.jasb.bind.id.IResolver;
import com.larkery.jasb.bind.id.Resolver;
import com.larkery.jasb.bind.read.IAtomReader;
import com.larkery.jasb.sexp.Atom;
import com.larkery.jasb.sexp.ISexpSource;
import com.larkery.jasb.sexp.ISexpVisitor;
import com.larkery.jasb.sexp.Invocation;
import com.larkery.jasb.sexp.Node;
import com.larkery.jasb.sexp.Seq;
import com.larkery.jasb.sexp.errors.BasicError;
import com.larkery.jasb.sexp.errors.IErrorHandler;
import com.larkery.jasb.sexp.errors.UnexpectedTermError;

/**
 * Not thread-safe.
 * @author hinton
 *
 */
public class Binder {
	private static final Logger log = LoggerFactory.getLogger(Binder.class);
	private static final String XREF_PREFIX = "#";
	final Multimap<String, ObjectMapping<?>> types = HashMultimap.create();
	final Set<IAtomReader> atomReaders;
	final IResolver resolver = new Resolver();
	final IErrorHandler errors;
	
	public static class Builder {
		final ImmutableSet.Builder<Class<?>> classes = ImmutableSet.builder();
		final ImmutableSet.Builder<IAtomReader> atomReaders = ImmutableSet.builder();
		IErrorHandler errors = IErrorHandler.SLF4J;
		
		public Binder build() {
			final Binder result = new Binder(errors, atomReaders.build());
			
			for (final Class<?> c : classes.build()) {
				result.addObjectMapping(ObjectMapping.of(errors, result, c));
			}
			
			return result;
		}
		
		public Builder addAtomReader(final IAtomReader reader) {
			atomReaders.add(reader);
			return this;
		}
		
		public Builder addClass(final Class<?> boundClass) {
			classes.add(boundClass);
			return this;
		}
	}
	
	public static Builder builder() {
		return new Builder();
	}
	
	Binder(final IErrorHandler errors, final Set<IAtomReader> readers) {
		this.errors = errors;
		atomReaders = ImmutableSet.copyOf(readers);
	}
	
	private void addObjectMapping(final ObjectMapping<?> mapping) {
		this.types.put(mapping.getName(), mapping);
	}
	
	/**
	 * Invoked when reading a node into a given type of output
	 * 
	 * @param node
	 * @param output
	 * @param errors
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <T> T read(
			final Node node, 
			final TypeToken<T> output) {
		final Object[] result = new Object[1];
		read(node, output, Callback.<T>store(errors, result));
		return (T) result[0];
	}
	
	public void write(final Object value, final ISexpVisitor visitor) {
		
	}
	
	public ISexpSource source(final Object value) {
		return new ISexpSource() {
			@Override
			public void accept(final ISexpVisitor visitor) {
				write(value, visitor);
			}
		};
	}
	
	protected <T> void read(final Node node, final TypeToken<T> output, final FutureCallback<T> result) {
		if (node instanceof Seq) {
			// this could be an invocation
			final Invocation invocation = Invocation.of(node, errors);
			if (invocation != null) {
				read(node, invocation, output, result);
			}
		} else if (node instanceof Atom) {
			readAtom((Atom)node, output, result);
		} else {
			errors.handle(BasicError.at(node, "unknown type of node"));
		}
	}
	
	/**
	 * Invoked when reading a node which is an invocation
	 * @param seq
	 * @param inv
	 * @param output
	 * @param errors
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private <T> void read(final Node seq , final Invocation inv, final TypeToken<T> output, final FutureCallback<T> result) {
		if (output.isAssignableFrom(TypeToken.of(Node.class))) {
			// return node directly
			result.onSuccess((T) seq);
			return;
		}
		
		for (final ObjectMapping<?> type : types.get(inv.name)) {
			if (output.isAssignableFrom(type.getBoundType())) {
				result.onSuccess((T) type.construct(inv));
				return;
			}
		}
		
		errors.handle(new UnexpectedTermError(seq, getLegalValuesForType(output), inv.name));
	}
	
	private Set<String> getLegalValuesForType(final TypeToken<?> output) {
		final TreeSet<String> legalValues = new TreeSet<>();
		
		for (final ObjectMapping<?> mapping : types.values()) {
			if (output.isAssignableFrom(mapping.getBoundType())) {
				legalValues.add(String.format("(%s)", mapping.getName()));
			}
		}
		
		for (final IAtomReader reader : atomReaders) {
			legalValues.addAll(reader.getLegalValues(output));
		}
		
		return legalValues;
	}
	
	@SuppressWarnings("unchecked")
	private <T> void readAtom(
			final Atom atom,
			final TypeToken<T> output,
			final FutureCallback<T> callback) {
		if (atom.getValue().startsWith(XREF_PREFIX)) {
			final String id = atom.getValue().substring(XREF_PREFIX.length());
			
			resolver.resolve(atom, id, output, callback);
		} else {
			if (output.isAssignableFrom(TypeToken.of(Node.class))) {
				// return node directly
				callback.onSuccess((T) atom);
				return;
			}
			
			log.debug("convert {} to {}", atom, output);
			for (final IAtomReader reader : atomReaders) {
				final Optional<T> val = reader.read(atom.getValue(), output);
				if (val.isPresent()) {
					callback.onSuccess(val.get());
					return;
				}
			}
			log.warn("could not convert {} to {}", atom, output);
			
			// produce a meaningful error here
			errors.handle(
					new UnexpectedTermError(atom, 
							getLegalValuesForType(output), 
							atom.getValue()));
		}
	}
}
