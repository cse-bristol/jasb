package com.larkery.jasb.bind.impl;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import com.google.common.util.concurrent.FutureCallback;
import com.larkery.jasb.bind.id.UnresolvableIdentifierException;
import com.larkery.jasb.sexp.errors.BasicError;
import com.larkery.jasb.sexp.errors.IErrorHandler;
import com.larkery.jasb.sexp.errors.UnresolvedReferenceError;

abstract class Callback<Q> implements FutureCallback<Q> {
	final IErrorHandler errors;
	
	private Callback(IErrorHandler errors) {
		super();
		this.errors = errors;
	}

	public static <Q> FutureCallback<Q> set(final IErrorHandler errors, final Object target, final Method setter) {
		return new Callback<Q>(errors) {
			@Override
			public void onSuccess(Q result) {
				try {
					setter.invoke(target, result);
				} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
					throw new RuntimeException(e);
				}
			}
		};
	}
	
	public static <Q> FutureCallback<Q> insert(final IErrorHandler errors, final List<Q> target, final int index) {
		return new Callback<Q>(errors) {
			@Override
			public void onSuccess(Q result) {
				target.set(index, result);
			}
		};
	}
	
	public static <Q> FutureCallback<Q> store(final IErrorHandler errors, final Object[] out) {
		return new Callback<Q>(errors) {
			@Override
			public void onSuccess(Q result) {
				out[0] = result;
			}
		};
	}
	
	@Override
	public void onFailure(Throwable t) {
		if (t instanceof UnresolvableIdentifierException) {
			errors.handle(new UnresolvedReferenceError(((UnresolvableIdentifierException) t).getAtom()));
		} else {
			errors.handle(BasicError.nowhere(t.getMessage()));
		}
	}
}
