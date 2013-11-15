package com.larkery.jasb.bind.id;

import com.google.common.util.concurrent.ListenableFuture;
import com.larkery.jasb.sexp.Atom;

public interface IResolver {
	public <Q> ListenableFuture<Q> resolve(
			final Atom cause,
			final String id, 
			final Class<Q> type);

	public void define(String result, Object o);
}
