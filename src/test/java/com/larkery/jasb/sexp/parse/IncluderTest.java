package com.larkery.jasb.sexp.parse;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.larkery.jasb.sexp.ISExpression;
import com.larkery.jasb.sexp.Seq;
import com.larkery.jasb.sexp.errors.IErrorHandler;
import com.larkery.jasb.sexp.parse.Includer.ILocationReader;
import com.larkery.jasb.sexp.parse.Includer.IResolver;

public class IncluderTest extends VisitingTest {
	private IResolver resolver;
	private Map<URI, String> values;
	@Before
	public void setup() {
		values = new HashMap<>();
		resolver = new IResolver() {
			
			@Override
			public ILocationReader resolve(final URI href, final IErrorHandler errors)
					throws NoSuchElementException {
				return Includer.stringLocationReader(href, values.get(href));
			}
			
			@Override
			public URI convert(final Seq include, final IErrorHandler errors) {
				final URI destination = URI.create("test://" + include.getTail().get(0).toString());
				
				return destination;
			}
		};
	}
	
	@Override
	protected ISExpression source(final String name, final String src) {
		values.put(URI.create("test://"+name), src);
		return Includer.source(
				resolver, 
				URI.create("test://"+name),
				RECORD
				);
	}
	
	@Test
	public void includesAreIncluded() {
		values.put(URI.create("test://my-include"), "hello");
		check("includesAreIncluded",
				"(include my-include)",
				e("hello")
				);
	}
	
	@Test
	public void recursiveIncludesAreIllegal() {
		values.put(URI.create("test://my-include"), "(include my-include)");
		check("includesAreIncluded",
				"(include my-include)",
				e("hello")
				);
		Assert.assertFalse(errors.isEmpty());
	}
	
	@Test
	public void collectingIncludesWorks() {
		values.put(URI.create("test://my-include"), "hello world");
		Includer.collectFromRoot(resolver, "(include my-include)", RECORD);
		
		
	}
	
	@Test
	public void includesHaveCorrectLocations() {
//		Assert.fail();
	}
}
