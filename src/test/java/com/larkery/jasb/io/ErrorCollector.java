package com.larkery.jasb.io;

import java.util.ArrayList;
import java.util.List;

import com.larkery.jasb.sexp.errors.IErrorHandler;

public class ErrorCollector implements IErrorHandler {
	public List<IError> errors = new ArrayList<>();
	
	@Override
	public void handle(final IError error) {
		errors.add(error);
	}
}
