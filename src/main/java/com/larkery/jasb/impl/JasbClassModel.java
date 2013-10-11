package com.larkery.jasb.impl;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.larkery.jasb.SChild;
import com.larkery.jasb.SElement;
import com.larkery.jasb.SRest;

class JasbClassModel {
	private final Class<?> clazz;
	private final String elementName;
	private final Map<String, JasbTypeDescriptor> descriptors;
	private final Optional<JasbTypeDescriptor> rest;

	public static JasbClassModel of(final Class<?> clazz, final Set<Class<?>> requirements) {
		final JasbClassModel model = new JasbClassModel(clazz);
		
		for (final JasbTypeDescriptor descriptor : model.descriptors.values()) {
			
		}
		
		if (model.rest.isPresent()) {
			
		}
		
		return model;
	}

	private JasbClassModel(final Class<?> clazz) {
		checkClass(clazz);
		
		this.clazz = clazz;
		
		final SElement element = clazz.getAnnotation(SElement.class);
		
		this.elementName = element.value();
		
		JasbTypeDescriptor rest = null;
		final ImmutableMap.Builder<String, JasbTypeDescriptor> descriptors = 
				ImmutableMap.builder();
		final HashSet<String> names = new HashSet<>();
		
		for (final Method method : clazz.getMethods()) {
			checkMethod(clazz, method);
			if (method.isAnnotationPresent(SChild.class)) {
				final JasbTypeDescriptor td = JasbTypeDescriptor.of(method);
				final String name = method.getAnnotation(SChild.class).value();
				if (names.contains(name)) {
					throw new JasbDefinitionException(clazz, 
							method.getName() + " redefines named child " + name);
				}
				names.add(name);
				descriptors.put(name, td);
			} else if (method.isAnnotationPresent(SRest.class)) {
				rest = JasbTypeDescriptor.of(method);
			}
		}
		
		this.rest = Optional.fromNullable(rest);
		this.descriptors = descriptors.build();
	}

	public Class<?> getElementClass() {
		return clazz;
	}
	
	public String getElementName() {
		return elementName;
	}
	
	private static void checkMethod(final Class<?> clazz, final Method method) {
		final boolean hasChildAnnotation = method.isAnnotationPresent(SChild.class);
		final boolean hasRestAnnotation = method.isAnnotationPresent(SRest.class);
		
		if (hasChildAnnotation && hasRestAnnotation) {
			throw new JasbDefinitionException(clazz, method.getName() + " has both SChild and SRest annotations, which is not cool");
		}
		
		if (method.getReturnType() == Void.TYPE) {
			throw new JasbDefinitionException(clazz, method.getName() + " is not a getter (it is void)");
		}
		
		if (method.getParameterTypes().length != 0) {
			throw new JasbDefinitionException(clazz, method.getName() + " is not a getter (it has arguments)");
		}
		
		final String property;
		if (method.getName().startsWith("is")) {
			property = method.getName().substring(2);
		} else if (method.getName().startsWith("get")) {
			property = method.getName().substring(3);
		} else {
			throw new JasbDefinitionException(clazz, method.getName() + " is not a property method (it doesn't start with get or is");
		}
		
		try {
			clazz.getMethod(property, method.getReturnType());
		} catch (final NoSuchMethodException nsme) {
			throw new JasbDefinitionException(clazz, method.getName() + " has no corresponding setter", nsme);
		}
		
		if (hasRestAnnotation) {
			for (final Method other : clazz.getMethods()) {
				if (other != method && other.isAnnotationPresent(SRest.class)) {
					throw new JasbDefinitionException(clazz, method.getName() + " " + other.getName() + " both have the SRest annotation, which is not cool");
				}			
			}
		}
	}

	private static void checkClass(Class<?> clazz2) {
		if (!clazz2.isAnnotationPresent(SElement.class)) {
			throw new JasbDefinitionException(clazz2, "SElement annotation required");
		}
	}
}
