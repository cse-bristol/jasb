package com.larkery.jasb.io;

import java.util.Set;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;

public class DoubleAtomIO implements IAtomReader,IAtomWriter {

	@Override
	public boolean canWrite(final Object object) {
		return object instanceof Double;
	}

	@Override
	public String write(final Object object) {
		return object.toString();
	}

	@Override
	public boolean canReadTo(final Class<?> output) {
		return output.isAssignableFrom(Double.class);
	}

	@Override
	public <T> Optional<T> read(final String in, final Class<T> out) {
		if (canReadTo(out)) {
			try {
				final double parseDouble = Double.parseDouble(in);
				return Optional.of(out.cast(parseDouble));
			} catch (final NumberFormatException nfe) {}	
		}
		return Optional.absent();
	}

	@Override
	public Set<String> getLegalValues(final Class<?> output) {
		return ImmutableSet.of("a real number");
	}

}
