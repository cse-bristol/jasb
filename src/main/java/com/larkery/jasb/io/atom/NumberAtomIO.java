package com.larkery.jasb.io.atom;

import java.util.Collections;
import java.util.Set;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.larkery.jasb.io.IAtomIO;

public class NumberAtomIO implements IAtomIO {

	@Override
	public boolean canWrite(final Object object) {
		return object instanceof Number;
	}

	@Override
	public String write(final Object object) {
		return object.toString();
	}

	@Override
	public boolean canReadTo(final Class<?> output) {
		return Number.class.isAssignableFrom(output);
	}

	@Override
	public <T> Optional<T> read(final String in, final Class<T> out) {
		if (out == Double.class) {
			try {
				final double parsed = Double.parseDouble(in);
				return Optional.of(out.cast(parsed));
			} catch (final NumberFormatException nfe) {}	
		} else if (out == Integer.class) {
			try {
				final int parsed = Integer.parseInt(in);
				return Optional.of(out.cast(parsed));
			} catch (final NumberFormatException nfe) {}	
		} else if (out == Float.class) {
			try {
				final float parsed = Float.parseFloat(in);
				return Optional.of(out.cast(parsed));
			} catch (final NumberFormatException nfe) {}	
		} else if (out == Long.class) {
			try {
				final long parsed = Long.parseLong(in);
				return Optional.of(out.cast(parsed));
			} catch (final NumberFormatException nfe) {}	
		}

		return Optional.absent();
	}

	@Override
	public Set<String> getLegalValues(final Class<?> out) {
		if (out == Double.class || out == Float.class) {
			return ImmutableSet.of("a real number");
		} else if (out == Integer.class || out == Long.class) {
			return ImmutableSet.of("a whole number");			
		} else {
			return Collections.emptySet();
		}
	}

}
