package com.larkery.jasb.bind.read;

import java.util.Set;

import com.google.common.collect.ImmutableSet;


public class StringAtomReader extends SimpleAtomReader<String> {
	@Override
	protected String convert(String in) {
		return in;
	}
	
	@Override
	protected Set<String> getLegalValues() {
		return ImmutableSet.of("a string");
	}
}
