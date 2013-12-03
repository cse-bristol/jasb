package com.larkery.jasb.sexp.errors;

import com.larkery.jasb.sexp.errors.IErrorHandler.IError;

public class JasbErrorException extends RuntimeException {
	private static final long serialVersionUID = 1L;
	private final IError error;

	public JasbErrorException(final IError error) {
		super(error.toString());
		this.error = error;
	}

	public IError getError() {
		return error;
	}
}
