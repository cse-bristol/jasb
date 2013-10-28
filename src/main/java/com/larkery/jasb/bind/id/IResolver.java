package com.larkery.jasb.bind.id;

import java.util.Set;

import com.google.common.reflect.TypeToken;
import com.google.common.util.concurrent.FutureCallback;
import com.larkery.jasb.sexp.Atom;

public interface IResolver {
	public <Q> void resolve(
			final Atom cause,
			final String id, 
			final TypeToken<Q> type, 
			final FutureCallback<Q> callback);
	
	public void pushBlock(final Set<Class<?>> holds);
	
	public void popBlock();
	
	public <Q> void define(
			final String id, 
			final TypeToken<Q> type,
			final Q value);
}
