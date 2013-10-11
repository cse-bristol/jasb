package com.larkery.jasb.impl;

import java.lang.reflect.Method;
import java.util.List;

import com.google.common.reflect.TypeToken;
import com.larkery.jasb.SChild;
import com.larkery.jasb.SRest;

public class JasbTypeDescriptor {
	private final Class<?> clazz;
	private final boolean exact;
	private final boolean multiple;

	static final TypeToken<List> listOfX
		= TypeToken.of(List.class);
	
	public static JasbTypeDescriptor of(final Method method) {
		return new JasbTypeDescriptor(method);
	}

	public JasbTypeDescriptor(final Method method) {
		if (method.isAnnotationPresent(SChild.class)) {
			exact = method.getAnnotation(SChild.class).exact();
		} else if (method.isAnnotationPresent(SRest.class)) {
			exact = method.getAnnotation(SRest.class).exact();
		} else {
			throw new IllegalArgumentException(method + " should not have got here");
		}
		
		final TypeToken<?> type = TypeToken.of(method.getGenericReturnType());
		
		if (listOfX.isAssignableFrom(type)) {
			// it is a list
			multiple = true;
		} else {
			// it is not a list
			multiple = false;
		}
	}
}
