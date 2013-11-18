package com.larkery.jasb.io;

import java.util.Collections;
import java.util.Set;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;

public class EnumAtomIO implements IAtomIO {

	@Override
	public boolean canWrite(final Object object) {
		return object.getClass().isEnum();
	}

	@Override
	public String write(final Object object) {
		return "" + object;
	}

	@Override
	public boolean canReadTo(final Class<?> output) {
		return output.isEnum();
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> Optional<T> read(final String in, final Class<T> out) {
		if (out.isEnum()) {
			for (final Object o : out.getEnumConstants()) {
				if (o.toString().equalsIgnoreCase(in)) {
					return Optional.<T>of((T) o);
				}
			}
		}
		return Optional.absent();
	}

	@Override
	public Set<String> getLegalValues(final Class<?> output) {
		if (output.isEnum()) {
			final ImmutableSet.Builder<String> builder = ImmutableSet.builder();

			for (final Object o : output.getEnumConstants()) {
				builder.add(o.toString());
			}
			
			return builder.build();

		} else {
			return Collections.emptySet();
		}
	}
}
