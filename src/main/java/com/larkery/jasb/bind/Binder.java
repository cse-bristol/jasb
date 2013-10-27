package com.larkery.jasb.bind;

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.reflect.TypeToken;
import com.larkery.jasb.bind.read.IAtomReader;
import com.larkery.jasb.sexp.Atom;
import com.larkery.jasb.sexp.IErrorHandler;
import com.larkery.jasb.sexp.Invocation;
import com.larkery.jasb.sexp.Node;
import com.larkery.jasb.sexp.Seq;

public class Binder {
	private static final Logger log = LoggerFactory.getLogger(Binder.class);
	final Multimap<String, BoundType<?>> types = HashMultimap.create();
	final Set<IAtomReader> atomReaders;
	
	public static Binder of(
			final Set<IAtomReader> readers, 
			final Class<?>... classes) {
		final HashSet<Class<?>> marks = new HashSet<>();
		final HashSet<BoundType<?>> out = new HashSet<>();
		for (final Class<?> c : classes) {
			collect(c, marks, out);
		}
		return new Binder(readers, out);
	}
	
	private static void collect(final Class<?> in, final Set<Class<?>> marked, final Set<BoundType<?>> out) {
		if (marked.contains(in)) {
			return;
		}
		
		marked.add(in);
		
		if (in.getAnnotation(Bind.class) != null) {
			final BoundType<?> t = new BoundType<>(in);
			out.add(t);
		}
	}
	
	private Binder(final Set<IAtomReader> readers, final Set<BoundType<?>> bound) {
		atomReaders = ImmutableSet.copyOf(readers);
		for (final BoundType<?> t : bound) {
			types.put(t.getName(), t);
		}
	}
	
	public <T> T read(final Node node, final TypeToken<T> output, final IErrorHandler errors) {
		if (node instanceof Seq) {
			// this could be an invocation
			final Invocation invocation = Invocation.of(node, errors);
			if (invocation != null) {
				return read(node, invocation, output, errors);
			}
		} else if (node instanceof Atom) {
			// this maybe an ID; IDs need handling differently.
			// we could say that refs are #thing
			// we should return a future here I think
			return readAtom((Atom)node, output, errors);
		} else {
			errors.error(node.getLocation(), "Unknown type of node");
		}
		return null;
	}
	
	@SuppressWarnings("unchecked")
	private <T> T read(final Node seq , final Invocation inv, final TypeToken<T> output, final IErrorHandler errors) {
		for (final BoundType<?> type : types.get(inv.name)) {
			if (output.isAssignableFrom(type.getBoundType())) {
				return (T) type.construct(this, inv, errors);
			}
		}
		
		//TODO better errors
		errors.error(seq.getLocation(), "unexpected term");
		return null;
	}
	
	private <T> T readAtom(final Atom atom, final TypeToken<T> output, final IErrorHandler errors) {
		log.debug("convert {} to {}", atom, output);
		for (final IAtomReader reader : atomReaders) {
			final Optional<T> val = reader.read(atom.getValue(), output);
			if (val.isPresent()) {
				return val.get();
			}
		}
		log.warn("could not convert {} to {}", atom, output);
		return null;
	}
}
