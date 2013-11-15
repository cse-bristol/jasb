package com.larkery.jasb.io;

import com.google.common.base.Optional;

public class StringAtomIO implements IAtomReader, IAtomWriter {

	@Override
	public boolean canReadTo(final Class<?> output) {
		return output.isAssignableFrom(String.class);
	}

	@Override
	public <T> Optional<T> read(final String in, final Class<T> out) {
		return Optional.of(out.cast(in));
	}

	@Override
	public boolean canWrite(final Object object) {
		return object instanceof String;
	}

	@Override
	public String write(final Object object) {
		return (String) object;
	}

}
