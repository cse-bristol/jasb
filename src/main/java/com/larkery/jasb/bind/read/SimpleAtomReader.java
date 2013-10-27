package com.larkery.jasb.bind.read;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.reflect.TypeToken;

public abstract class SimpleAtomReader<Q> implements IAtomReader {
	private static final Logger log = LoggerFactory.getLogger(SimpleAtomReader.class);
	private final TypeToken<?> myType;
	
	SimpleAtomReader() {
		try {
			myType = TypeToken.of(
					getClass().getDeclaredMethod("convert", String.class).getGenericReturnType()
					);
		} catch (NoSuchMethodException | SecurityException e) {
			throw new RuntimeException(e);
		}
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <Out> Optional<Out> read(String in, TypeToken<Out> out) {
		final boolean canConvert;
		
		if (out.getRawType().isPrimitive()) {
			final Class<? super Out> oc = out.getRawType();
			if (oc == byte.class) {
				canConvert = TypeToken.of(Byte.class).isAssignableFrom(myType);
			} else if (oc == short.class) {
				canConvert = TypeToken.of(Short.class).isAssignableFrom(myType);
			} else if (oc == int.class) {
				canConvert = TypeToken.of(Integer.class).isAssignableFrom(myType);
			} else if (oc == long.class) {
				canConvert = TypeToken.of(Long.class).isAssignableFrom(myType);
			} else if (oc == float.class) {
				canConvert = TypeToken.of(Float.class).isAssignableFrom(myType);
			} else if (oc == double.class) {
				canConvert = TypeToken.of(Double.class).isAssignableFrom(myType);
			} else if (oc == boolean.class) {
				canConvert = TypeToken.of(Boolean.class).isAssignableFrom(myType);
			} else if (oc == char.class) {
				canConvert = TypeToken.of(Character.class).isAssignableFrom(myType);
			} else {
				throw new RuntimeException("Unknown primitive type " + oc);
			}
		} else {
			canConvert = out.isAssignableFrom(myType);
		}
		
		if (canConvert) {
			log.trace("<{}> can convert to {}", myType, out);
			return Optional.fromNullable(
					(Out)
					convert(in));
		} else {
			log.trace("<{}> cannot convert to {}", myType, out);
			return Optional.absent();
		}
	}

	protected abstract Q convert(final String in);
}
