package com.larkery.jasb.bind.read;

import java.util.Collections;
import java.util.Set;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.TypeToken;

public class EnumAtomReader implements IAtomReader {
	@SuppressWarnings("unchecked")
	@Override
	public <Out> Optional<Out> read(String in, TypeToken<Out> out) {
		if (out.getRawType().isEnum()) {
			for (final Object o : out.getRawType().getEnumConstants()) {
				if (o.toString().equalsIgnoreCase(in)) {
					return Optional.<Out>of((Out) o);
				}
			}
		}
		return Optional.absent();
	}
	
	@Override
	public <Out> Set<String> getLegalValues(final TypeToken<Out> out) {
		if (out.getRawType().isEnum()) {
			final ImmutableSet.Builder<String> builder = ImmutableSet.builder();

			for (final Object o : out.getRawType().getEnumConstants()) {
				builder.add(o.toString());
			}
			
			return builder.build();
		} else {
			return Collections.emptySet();
		}
	}
}
