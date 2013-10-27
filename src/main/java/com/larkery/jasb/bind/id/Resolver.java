package com.larkery.jasb.bind.id;

import java.util.HashMap;
import java.util.Map;

import com.google.common.reflect.TypeToken;
import com.google.common.util.concurrent.FutureCallback;

public class Resolver implements IResolver {
	static class Pending<Q> {
		private final TypeToken<Q> type;
		private final FutureCallback<Q> callback;
		private boolean done; 
		
		public Pending(TypeToken<Q> type, FutureCallback<Q> callback) {
			super();
			this.type = type;
			this.callback = callback;
		}
		
		public void handle(final Q value) {
			if (!done) {
				callback.onSuccess(value);
				done = true; 
			}
		}
	}
	
	static class Scope {
		private final Map<String, Object> contents = new HashMap<>();
		
		
		public void define(final String id, final Object object) {
			contents.put(id, object);
		}
		
		public <Q> void resolve(final Pending<Q> pending) {
			
		}
	}
	
	public Resolver() {
		
	}
	
	@Override
	public <Q> void resolve(
			final String id, 
			final TypeToken<Q> type, 
			final FutureCallback<Q> callback) {
		
	}

	@Override
	public void pushDefinitionBlock() {
		
	}

	@Override
	public void popDefinitionBlock() {
		
	}

	@Override
	public void defineLocally(final String id, Object value) {

	}

	@Override
	public void defineGlobally(final String id, Object value) {
		
	}
}
