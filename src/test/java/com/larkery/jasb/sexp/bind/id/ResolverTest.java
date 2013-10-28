package com.larkery.jasb.sexp.bind.id;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.TypeToken;
import com.google.common.util.concurrent.FutureCallback;
import com.larkery.jasb.bind.id.Resolver;

public class ResolverTest {
	static class Callback<Q> implements FutureCallback<Q> {
		public Optional<Q> result = Optional.absent();

		@Override
		public void onSuccess(Q result) {
			this.result = Optional.of(result);
		}

		@Override
		public void onFailure(Throwable t) {
			
		}
	}
	
	@Test
	public void testSimpleResolution() {
		final Resolver res = new Resolver();
		res.pushBlock(ImmutableSet.<Class<?>>of(Object.class));
		
		final Object o1 = new Object();
		final Object o2 = new Object();
		res.define("first", TypeToken.of(Object.class), o1);
		final Callback<Object> cb1 = new Callback<>();
		final Callback<Object> cb2 = new Callback<>();
		
		res.resolve(null, "first", TypeToken.of(Object.class), cb1);
		res.resolve(null, "second", TypeToken.of(Object.class), cb2);
		
		res.define("second", TypeToken.of(Object.class), o2);
		
		res.popBlock();
		
		Assert.assertSame(o1, cb1.result.get());
		Assert.assertSame(o2, cb2.result.get());
	}
	
	@Test
	public void testMatchingResolution() {
		final Resolver res = new Resolver();
		res.pushBlock(ImmutableSet.<Class<?>>of(Object.class));
		
		res.pushBlock(ImmutableSet.<Class<?>>of(String.class));
		
		res.define("first", TypeToken.of(String.class), "Hello");
		
		final Callback<Object> cb1 = new Callback<>();
		
		final Object o1 = new Object();
		
		res.resolve(null, "second", TypeToken.of(Object.class), cb1);
		
		res.popBlock();
		
		res.define("second", TypeToken.of(Object.class), o1);
		
		res.define("first", TypeToken.of(String.class), "World");
		
		final Callback<Object> cb2 = new Callback<>();
		
		res.resolve(null, "first", TypeToken.of(Object.class), cb2);
		
		Assert.assertSame(o1, cb1.result.get());
		Assert.assertEquals("World", cb2.result.get());
	}
}
