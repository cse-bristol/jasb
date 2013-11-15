package com.larkery.jasb.io;

import java.util.Set;

import org.joda.time.Period;
import org.joda.time.format.PeriodFormat;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;

public class PeriodAtomIO implements IAtomReader,IAtomWriter {

	@Override
	public boolean canWrite(final Object object) {
		return object instanceof Period;
	}

	@Override
	public String write(final Object object) {
		return PeriodFormat.getDefault().print((Period) object);
	}

	@Override
	public boolean canReadTo(final Class<?> output) {
		return output.isAssignableFrom(Period.class);
	}

	@Override
	public <T> Optional<T> read(final String in, final Class<T> out) {
		try {
			final Period parsePeriod = PeriodFormat.getDefault().parsePeriod(in);
			return Optional.of(out.cast(parsePeriod));
		} catch (final IllegalArgumentException e) {
			return Optional.absent();
		}
	}

	@Override
	public Set<String> getLegalValues(final Class<?> output) {
		return ImmutableSet.of("a period (1 year, 2 days, etc)");
	}
}
