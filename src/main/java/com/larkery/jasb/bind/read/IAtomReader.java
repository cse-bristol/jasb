package com.larkery.jasb.bind.read;

import java.util.Set;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.TypeToken;

public interface IAtomReader {
	public final Set<IAtomReader> PRIMITIVES = 
			ImmutableSet.<IAtomReader>of(
					new StringAtomReader(),
					new DoubleAtomReader()
					);
	public <Out> Optional<Out> read(final String in, final TypeToken<Out> out);
	
	public <Out> Set<String> getLegalValues(final TypeToken<Out> out);
}
