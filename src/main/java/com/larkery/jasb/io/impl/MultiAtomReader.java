package com.larkery.jasb.io.impl;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.larkery.jasb.io.IAtomReader;
import com.larkery.jasb.sexp.Atom;

class MultiAtomReader<T> {
	private final Class<T> clazz;
	private final ImmutableSet<IAtomReader> delegates;

	public MultiAtomReader(final Class<T> clazz, final ImmutableSet<IAtomReader> build) {
		this.clazz = clazz;
		this.delegates = build;
	}

	protected ListenableFuture<T> read(final Atom atom) {
		for (final IAtomReader reader : delegates) {
			final Optional<T> value = reader.read(atom.getValue(), clazz);
			if (value.isPresent()) {
				return Futures.immediateFuture(value.get());
			}
		}
		
		//TODO generate illegal values warnings here
		return Futures.immediateFailedFuture(new RuntimeException("Could not read " + atom + " as " + clazz));
	}

}
