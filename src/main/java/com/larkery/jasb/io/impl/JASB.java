package com.larkery.jasb.io.impl;

import java.lang.reflect.Modifier;
import java.util.Set;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableSet;
import com.larkery.jasb.io.IAtomIO;
import com.larkery.jasb.io.IModel;
import com.larkery.jasb.io.IReader;
import com.larkery.jasb.io.IWriter;
/**
 * An entry point for jasb
 *
 */
public class JASB {
	private final IReader reader;
	private final IWriter writer;
	private final Model model;

	JASB(final Set<Class<?>> classes, final Set<IAtomIO> atoms) {
		final Set<Class<?>> concrete = ImmutableSet.copyOf(Collections2.filter(classes, 
				new Predicate<Class<?>>() {
					@Override
					public boolean apply(final Class<?> input) {
						return !Modifier.isAbstract(input.getModifiers());
					}
				}
				));
		
		this.reader = new Reader(concrete, atoms);
		this.model = new Model(concrete, atoms);
		this.writer = new Writer(atoms);
	}

	public IWriter getWriter() {
		return writer;
	}
	
	public IReader getReader() {
		return reader;
	}
	
	public IModel getModel() {
		return this.model;
	}

	public static JASB of(final Set<Class<?>> classes, final Set<IAtomIO> atoms) {
		return new JASB(classes, atoms);
	}
}
