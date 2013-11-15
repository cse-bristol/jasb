package com.larkery.jasb.io;

import com.google.common.base.Optional;

public interface IAtomReader {
	public boolean canReadTo(final Class<?> output);
	public <T> Optional<T> read(final String in, final Class<T> out);
}
