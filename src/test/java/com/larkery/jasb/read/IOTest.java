package com.larkery.jasb.read;

import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;
import com.larkery.jasb.bind.Bind;
import com.larkery.jasb.bind.BindNamedArgument;
import com.larkery.jasb.bind.BindPositionalArgument;
import com.larkery.jasb.bind.BindRemainingArguments;
import com.larkery.jasb.bind.id.Identity;
import com.larkery.jasb.io.IAtomReader;
import com.larkery.jasb.io.IAtomWriter;
import com.larkery.jasb.io.StringAtomIO;
import com.larkery.jasb.io.impl.ReadContext;
import com.larkery.jasb.io.impl.Writer;
import com.larkery.jasb.sexp.Location.Type;
import com.larkery.jasb.sexp.Node;
import com.larkery.jasb.sexp.parse.Parser;

public class IOTest {
	@Bind("hello")
	public static class Thingy {
		private List<String> val = new ArrayList<>();

		private String name;
		
		@Identity
		@BindNamedArgument("name")
		public String getName() {
			return name;
		}

		public void setName(final String name) {
			this.name = name;
		}

		@BindNamedArgument("val")
		public List<String> getVal() {
			return val;
		}

		public void setVal(final List<String> val) {
			this.val = val;
		}
		
		private String bip;
		
		private List<Thingy> xref = new ArrayList<>();
		
		@BindRemainingArguments
		public List<Thingy> getXref() {
			return xref;
		}

		public void setXref(final List<Thingy> xref) {
			this.xref = xref;
		}

		@BindPositionalArgument(0)
		public String getBip() {
			return bip;
		}

		public void setBip(final String bip) {
			this.bip = bip;
		}
	}
	
	ReadContext context;
	
	@Before
	public void setup() throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		context = new ReadContext(ImmutableSet.<Class<?>>of(Thingy.class), ImmutableSet.<IAtomReader>of(new StringAtomIO()));
	}
	
	
	@Test
	public void readReturnsNewThingy() throws InterruptedException, ExecutionException {
		final Node inputNode = Node.copy(Parser.source(Type.Normal, "nowhere", new StringReader("(hello name:foo val:(10 20 30) 9 #foo #foo)"), null));
		final Thingy thingy = context.read(
				Thingy.class, 
				inputNode).get();
		Assert.assertNotNull(thingy);
		
		final Writer writer = new Writer(ImmutableSet.<IAtomWriter>of(new StringAtomIO()));
		
//		writer.write(thingy).accept(new PrintVisitor(System.out));
		
//		final Node outputNode = Node.copy(writer.write(thingy));
		
//		Assert.assertEquals(inputNode, outputNode);
		
//		outputNode.accept(new PrintVisitor(System.out));
	}
}
