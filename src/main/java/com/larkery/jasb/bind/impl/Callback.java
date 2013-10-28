package com.larkery.jasb.bind.impl;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import com.google.common.util.concurrent.FutureCallback;

abstract class Callback<Q> implements FutureCallback<Q> {
	public static <Q> FutureCallback<Q> set(final Object target, final Method setter) {
		return new Callback<Q>() {
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
	
	public static <Q> FutureCallback<Q> insert(final List<Q> target, final int index) {
		return new Callback<Q>() {
			@Override
			public void onSuccess(Q result) {
				target.set(index, result);
			}
		};
	}
	
	
	@Override
	public void onFailure(Throwable t) {}
}
