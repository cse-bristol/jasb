package com.larkery.jasb.io.impl;

import java.util.HashMap;
import java.util.Map;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.larkery.jasb.sexp.Atom;


/**
 * Resolves a single big flat namespace
 * @author hinton
 *
 */
class Resolver  {
	private final Map<String, SettableFuture<?>> futures = new HashMap<>();
	private final Map<String, Class<?>> futureClasses = new HashMap<>();
	
	@SuppressWarnings("unchecked")

	public <Q> ListenableFuture<Q> resolve(final Atom cause, final String id, final Class<Q> type) {
		if (!futures.containsKey(id)) {
			futures.put(id, SettableFuture.create());
			futureClasses.put(id, type);
		}
		
		if (type.isAssignableFrom(futureClasses.get(id))) {
			return (ListenableFuture<Q>) futures.get(id);
		} else {
			return Futures.immediateFailedFuture(new RuntimeException("Incompatible definitions for " + id));
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })

	public void define(final String result, final Object o) {
		if (futures.containsKey(result)) {
			if (futures.get(result).isDone()) {
				throw new RuntimeException(result + " defined twice!");
			} else {
				if (futureClasses.get(result).isInstance(o)) {
					((SettableFuture) futures.get(result)).set(o);
				}
			}
		} else {
			futures.put(result, SettableFuture.create());
			futureClasses.put(result, o.getClass());
			((SettableFuture) futures.get(result)).set(o);
		}
	}
	
}
