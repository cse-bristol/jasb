package com.larkery.jasb.io;

import java.util.Set;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;

public class DateAtomIO implements IAtomReader,IAtomWriter {
	 private final DateTimeFormatter df =  DateTimeFormat.forPattern("dd/MM/yyyy");

	@Override
	public boolean canWrite(final Object object) {
		return object instanceof DateTime;
	}

	@Override
	public String write(final Object object) {
		return df.print((DateTime) object);
	}

	@Override
	public boolean canReadTo(final Class<?> output) {
		return output.isAssignableFrom(DateTime.class);
	}

	@Override
	public <T> Optional<T> read(final String in, final Class<T> out) {
		if (in == null || in.trim().isEmpty()) return Optional.absent(); 
		try {
			final T res = out.cast(df.parseDateTime(in.trim()));
			return Optional.of(res);
		} catch (final IllegalArgumentException e) {
			return Optional.absent();
		}
	}

	@Override
	public Set<String> getLegalValues(final Class<?> output) {
		return ImmutableSet.of("a date (dd/MM/yyyy)");
	}

}
