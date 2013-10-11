package com.larkery.jasb.impl;

import java.lang.reflect.Modifier;
import java.util.Iterator;
import java.util.LinkedHashSet;

import com.google.common.collect.ImmutableList;

public class JasbContext implements IJasbContext {
	public JasbContext(final Class<?>[] classes) {
		final LinkedHashSet<Class<?>> toProcess = new LinkedHashSet<>(ImmutableList.copyOf(classes));
		final Iterator<Class<?>> it = toProcess.iterator();
		while (it.hasNext()) {
			final Class<?> clazz = it.next();
			if (Modifier.isAbstract(clazz.getModifiers())) continue;
			final JasbClassModel model = JasbClassModel.of(it.next(), toProcess);
		}
	}

	public static IJasbContext create(final Class<?>... classes) {
		return new JasbContext(classes);
	}
	
	public <T> T unmarshal(final Something tree, final Class<T> output) {
		// get closest jasb class model and ask it to unserialize
	}
	
	public Something marshal(final Object value) {
		// get closest jasb class model, and ask it to serialize
	}
}
