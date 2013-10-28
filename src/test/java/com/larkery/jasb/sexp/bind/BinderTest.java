package com.larkery.jasb.sexp.bind;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.reflect.TypeToken;
import com.larkery.jasb.bind.Bind;
import com.larkery.jasb.bind.BindNamedArgument;
import com.larkery.jasb.bind.BindRemainingArguments;
import com.larkery.jasb.bind.id.CreatesReferenceScope;
import com.larkery.jasb.bind.id.Identity;
import com.larkery.jasb.bind.impl.Binder;
import com.larkery.jasb.bind.read.DoubleAtomReader;
import com.larkery.jasb.bind.read.StringAtomReader;
import com.larkery.jasb.sexp.IErrorHandler;
import com.larkery.jasb.sexp.Node;
import com.larkery.jasb.sexp.parse.Parser;

public class BinderTest {
	@Bind("scenario")
	@CreatesReferenceScope("globals")
	public static class Scenario {
		private String name;
		private double value;
		
		private List<Plus> plus = new ArrayList<>();
		
		public Scenario() {
			
		}
		@BindNamedArgument("name")
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		
		@BindNamedArgument("value")
		public double getValue() {
			return value;
		}
		public void setValue(double value) {
			this.value = value;
		}
		
		@BindNamedArgument("plus")
		public List<Plus> getPlus() {
			return plus;
		}
		public void setPlus(List<Plus> plus) {
			this.plus = plus;
		}
	}
	
	@Bind("+")
	public static class Plus {
		private String name;
		private List<Double> values = new ArrayList<>();

		@Identity
		@BindNamedArgument("name")
		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		@BindRemainingArguments
		public List<Double> getValues() {
			return values;
		}

		public void setValues(List<Double> values) {
			this.values = values;
		}
	}

	private Binder binder;
	private IErrorHandler slf4j;
	
	@Before
	public void setup() {
		this.binder =
				Binder.builder()
				.addAtomReader(new StringAtomReader())
				.addAtomReader(new DoubleAtomReader())
				.addClass(Scenario.class)
				.addClass(Plus.class).build();
	}
	
	@Test
	public void bindsScenario() {
		final Node node = Node.copy(Parser.source(
				"scenario test", 
				new StringReader("(scenario name:jake value:10.3 plus:((+ name:foo 1 2 3) #foo))")
				, slf4j));
		
		final Scenario read = binder.read(node, TypeToken.of(Scenario.class));
		Assert.assertEquals("jake", read.getName());
		Assert.assertEquals(Double.parseDouble("10.3"), read.getValue(), 0);
		Assert.assertEquals(2, read.getPlus().size());
		Assert.assertSame(read.getPlus().get(0), read.getPlus().get(1));
		
		Assert.assertEquals(ImmutableList.of(1d,2d,3d), read.getPlus().get(0).getValues());
	}
	
	@Test
	public void bindsPlus() {
		final Node node = Node.copy(Parser.source(
				"plus test", 
				new StringReader("(+ 1 2 3 4 5)")
				, slf4j));
		
		final Plus read = binder.read(node, TypeToken.of(Plus.class));
		Assert.assertEquals(ImmutableList.of(1d,2d,3d,4d,5d), read.getValues());
	}
}
