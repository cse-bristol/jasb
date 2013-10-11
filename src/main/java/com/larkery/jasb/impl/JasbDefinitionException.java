package com.larkery.jasb.impl;

public class JasbDefinitionException extends RuntimeException {
	private static final long serialVersionUID = 1L;
	private final Class<?> clazz;
	
	public JasbDefinitionException(final Class<?> clazz, String message, Throwable cause) {
		super(message, cause);
		this.clazz = clazz;
	}

	public JasbDefinitionException(final Class<?> clazz, String message) {
		super(message);
		this.clazz = clazz;
	}

	public Class<?> getJasbClass() {
		return clazz;
	}
}
