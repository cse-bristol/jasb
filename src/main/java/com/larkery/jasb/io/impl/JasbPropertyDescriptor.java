package com.larkery.jasb.io.impl;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.TypeToken;
import com.larkery.jasb.bind.BindNamedArgument;
import com.larkery.jasb.bind.BindPositionalArgument;
import com.larkery.jasb.bind.BindRemainingArguments;
import com.larkery.jasb.bind.id.Identity;

class JasbPropertyDescriptor {
	public final boolean isMultiple;
	public final Class<?> propertyType;
	public final Method readMethod;
	public final Method writeMethod;
	public final String name;
	public final BoundTo boundTo;
	
	public final Optional<String> key;
	public final Optional<Integer> position;
	public final Class<?> boxedPropertyType;
	public final boolean isIdentifier;
	
	public enum BoundTo {
		Name,
		Position,
		Remainder
	}
	
	@Override
	public String toString() {
		return "\"" + name + "\"";
	}
	
	public static Set<JasbPropertyDescriptor> getPropertiesBoundTo(final JasbPropertyDescriptor.BoundTo t, final Iterable<JasbPropertyDescriptor> properties) {
		final ImmutableSet.Builder<JasbPropertyDescriptor> out = ImmutableSet.builder();
		for (final JasbPropertyDescriptor p : properties) {
			if (p.boundTo == t) out.add(p);
		}
		return out.build();
	}
	
	public static Set<JasbPropertyDescriptor> getDescriptors(final Class<?> clazz) {
		try {
			final ImmutableSet.Builder<JasbPropertyDescriptor> out = ImmutableSet.builder();
			for (final PropertyDescriptor pd : Introspector.getBeanInfo(clazz).getPropertyDescriptors()) {
				final Optional<JasbPropertyDescriptor> o = getDescriptor(pd);
				if (o.isPresent()) {
					out.add(o.get());
				}
			}
			return out.build();
		} catch (final IntrospectionException e) {
			throw new RuntimeException("Could not get bean info for " +clazz);
		}
	}
	
	public static Optional<JasbPropertyDescriptor> getDescriptor(final PropertyDescriptor pd) {
		final Method reader = pd.getReadMethod();
		if (reader != null && pd.getWriteMethod() != null) {
			if (reader.isAnnotationPresent(BindNamedArgument.class) ||
			    reader.isAnnotationPresent(BindPositionalArgument.class) ||
			    reader.isAnnotationPresent(BindRemainingArguments.class)) {
				
				return Optional.of(new JasbPropertyDescriptor(pd));
			}
		}
		return Optional.absent();
	}
	
	private static Class<?> getBoxedType(final Class<?> t) {
		if (t.isPrimitive()) {
			if (t == int.class) {
				return Integer.class;
			} else if (t == double.class) {
				return Double.class;
			} else if (t == boolean.class) {
				return Boolean.class;
			} else if (t == long.class) {
				return Long.class;
			} else if (t == float.class) {
				return Float.class;
			} else if (t == char.class) {
				return Character.class;
			} else if (t == byte.class) {
				return Byte.class;
			} else if (t == short.class) {
				return Short.class;
			} else {
				throw new IllegalArgumentException(t + " is a new kind of primitive type! Quite suprised.");
			}
		} else {
			return t;
		}
	}
	
	JasbPropertyDescriptor(final PropertyDescriptor pd) {
		isMultiple = List.class.isAssignableFrom(pd.getPropertyType());
		isIdentifier = pd.getReadMethod().isAnnotationPresent(Identity.class);
		if (isMultiple) {
			propertyType = getListTypeParameter(pd.getReadMethod().getGenericReturnType());
		} else {
			propertyType = pd.getPropertyType();
		}
		
		boxedPropertyType = getBoxedType(propertyType);
		
		readMethod = pd.getReadMethod();
		writeMethod = pd.getWriteMethod();
		name = pd.getName();
		
		if (readMethod.isAnnotationPresent(BindNamedArgument.class)) {
			boundTo = BoundTo.Name;
			key = Optional.of(readMethod.getAnnotation(BindNamedArgument.class).value());
			position = Optional.<Integer>absent();
		} else if (readMethod.isAnnotationPresent(BindPositionalArgument.class)) {
			boundTo = BoundTo.Position;
			key = Optional.<String>absent();
			position = Optional.<Integer>of(readMethod.getAnnotation(BindPositionalArgument.class).value());
		} else {
			boundTo = BoundTo.Remainder;
			key = Optional.<String>absent();
			position = Optional.<Integer>absent();
		}
		
		checkConsistency();
	}
	
	private void checkConsistency() {
		switch (boundTo) {
		case Name:
			break;
		case Position:
			break;
		case Remainder:
			if (!isMultiple) {
				throw new RuntimeException(this + " is bound to remaining arguments, but is not list typed. This is unsupported - remaining arguments must always be a list of things.");
			}
			break;
		default:
			break;
		}
	}

	private static Class<?> getListTypeParameter(final java.lang.reflect.Type genericReturnType) {
		try {
			return TypeToken.of(genericReturnType)
					.resolveType(
							List.class.getMethod("get", int.class)
							.getGenericReturnType()).getRawType();
		} catch (NoSuchMethodException | SecurityException e) {
			throw new RuntimeException("Cannot see list.get");
		}
	}
}
