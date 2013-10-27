package com.larkery.jasb.bind.id;

import com.google.common.reflect.TypeToken;
import com.google.common.util.concurrent.FutureCallback;

public interface IResolver {
	public <Q> void resolve(
			final String id, 
			final TypeToken<Q> type, 
			final FutureCallback<Q> callback);
	
	public void pushDefinitionBlock(String type);
	
	public void popDefinitionBlock();
	
	public void defineInBlock(
			final String type, 
			final String id, 
			final Object value);
}
