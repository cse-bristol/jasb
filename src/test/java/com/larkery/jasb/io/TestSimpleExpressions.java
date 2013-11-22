package com.larkery.jasb.io;

import java.io.StringReader;
import java.net.URI;
import java.util.concurrent.ExecutionException;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.larkery.jasb.io.impl.Reader;
import com.larkery.jasb.io.testmodel.Arithmetic;
import com.larkery.jasb.io.testmodel.Div;
import com.larkery.jasb.io.testmodel.GetNode;
import com.larkery.jasb.io.testmodel.Plus;
import com.larkery.jasb.io.testmodel.Times;
import com.larkery.jasb.io.testmodel.Value;
import com.larkery.jasb.sexp.Location.Type;
import com.larkery.jasb.sexp.Node;
import com.larkery.jasb.sexp.errors.IErrorHandler;
import com.larkery.jasb.sexp.parse.Parser;

public class TestSimpleExpressions {
	public <T> T read(final String s, final Class<T> out) throws InterruptedException, ExecutionException {
		final Reader context = 
				new Reader(
						ImmutableSet.<Class<?>>of(
								GetNode.class,
								Div.class,
								Plus.class,
								Times.class,
								Value.class),
						ImmutableSet.of(
								new StringAtomIO(),
								new NumberAtomIO()));
		
		final Node node = 
				Node.copy(Parser.source(Type.Normal, URI.create("test"), new StringReader(s), IErrorHandler.SLF4J));
		
		final T result = context.getContext(IErrorHandler.SLF4J).read(out, node).get();
		
		if (result instanceof Arithmetic) {
			Assert.assertSame(node, ((Arithmetic) result).node);
		}
		
		return result;
	}
	
	@Test
	public void readsValue() throws InterruptedException, ExecutionException {
		final Value read = read("(value)", Value.class);
		Assert.assertNotNull(read);
	}
	
	@Test
	public void readsValueWithDouble() throws InterruptedException, ExecutionException {
		final Value read = read("(value of:1)", Value.class);
		Assert.assertNotNull(read);
		Assert.assertEquals(1, read.value, 0);
	}
	
	@Test
	public void readsEmptyPlus() throws InterruptedException, ExecutionException {
		final Plus read = read("(+)", Plus.class);
		Assert.assertEquals(ImmutableList.of(), read.terms);
	}
	
	@Test
	public void readsEmptyTimes() throws InterruptedException, ExecutionException {
		final Times read = read("(*)", Times.class);
		Assert.assertEquals(ImmutableList.of(), read.terms);
	}
	
	@Test
	public void readsDiv() throws InterruptedException, ExecutionException {
		final Div read = read("(/)", Div.class);
		Assert.assertNotNull(read);
	}
	
	@Test
	public void readsDivWithArguments() throws InterruptedException, ExecutionException {
		final Div read = read("(/ (value of: 1) (value of:2))", Div.class);
		Assert.assertNotNull(read);
		Assert.assertNotNull(read.first);
		Assert.assertNotNull(read.second);
		Assert.assertEquals(1, ((Value) read.first).value, 0);
		Assert.assertEquals(2, ((Value) read.second).value, 0);
	}
	
	@Test
	public void readsNodeWithoutBinding() throws InterruptedException, ExecutionException {
		final GetNode node = read("(get (+ (value of: 1) (value of: 2)))", GetNode.class);
		Assert.assertEquals("(+ (value of: 1) (value of: 2))", node.node.toString());
	}
}
