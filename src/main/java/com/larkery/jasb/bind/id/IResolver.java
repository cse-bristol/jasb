package com.larkery.jasb.bind.id;

import java.util.Set;

import com.google.common.reflect.TypeToken;
import com.google.common.util.concurrent.FutureCallback;

public interface IResolver {
	public <Q> void resolve(
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
