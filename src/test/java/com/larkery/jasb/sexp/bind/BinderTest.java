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
import com.larkery.jasb.bind.Binder;
import com.larkery.jasb.bind.read.IAtomReader;
import com.larkery.jasb.sexp.IErrorHandler;
import com.larkery.jasb.sexp.Node;
import com.larkery.jasb.sexp.parse.Parser;

public class BinderTest {
	@Bind("scenario")
	public static class Scenario {
		private String name;
		private double value;
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
	}
	
	@Bind("+")
	public static class Plus {
		private List<Double> values = new ArrayList<>();

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
		this.binder = Binder.of(
				IAtomReader.PRIMITIVES,
				Scenario.class, Plus.class);
		slf4j = IErrorHandler.SLF4J;
	}
	
	@Test
	public void bindsScenario() {
		final Node node = Node.copy(Parser.source(
				"test", 
				new StringReader("(scenario name:jake value:10.3)")
				, slf4j));
		
		final Scenario read = binder.read(node, TypeToken.of(Scenario.class), slf4j);
		Assert.assertEquals("jake", read.getName());
		Assert.assertEquals(Double.parseDouble("10.3"), read.getValue(), 0);
	}
	
	@Test
	public void bindsPlus() {
		final Node node = Node.copy(Parser.source(
				"test", 
				new StringReader("(+ 1 2 3 4 5)")
				, slf4j));
		
		final Plus read = binder.read(node, TypeToken.of(Plus.class), slf4j);
		Assert.assertEquals(ImmutableList.of(1d,2d,3d,4d,5d), read.getValues());
	}
}
