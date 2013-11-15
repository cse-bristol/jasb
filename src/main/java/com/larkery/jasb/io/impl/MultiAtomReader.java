package com.larkery.jasb.io.impl;

import java.util.Set;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.larkery.jasb.io.IAtomReader;
import com.larkery.jasb.io.IReadContext;
import com.larkery.jasb.sexp.Atom;
import com.larkery.jasb.sexp.errors.UnexpectedTermError;

class MultiAtomReader<T> {
	private final Class<T> clazz;
	private final ImmutableSet<IAtomReader> delegates;
	private final Set<String> legalValues;

	public MultiAtomReader(final Class<T> clazz, final ImmutableSet<IAtomReader> build) {
		this.clazz = clazz;
		this.delegates = build;
		final ImmutableSet.Builder<String> builder = ImmutableSet.builder();
		for (final IAtomReader delegate : delegates) {
			builder.addAll(delegate.getLegalValues(clazz));
		}
		legalValues = builder.build();
	}

	protected ListenableFuture<T> read(final IReadContext context, final Atom atom) {
		for (final IAtomReader reader : delegates) {
			final Optional<T> value = reader.read(atom.getValue(), clazz);
			if (value.isPresent()) {
				return Futures.immediateFuture(value.get());
			}
		}
		
		context.handle(
				new UnexpectedTermError(atom, legalValues, atom.getValue()));
		
		return Futures.immediateFailedFuture(new RuntimeException("Could not read " + atom + " as " + clazz));
	}

}
