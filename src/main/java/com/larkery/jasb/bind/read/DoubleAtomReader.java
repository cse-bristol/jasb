package com.larkery.jasb.bind.read;

import java.util.Set;

import com.google.common.collect.ImmutableSet;


public class DoubleAtomReader extends SimpleAtomReader<Double> {
	@Override
	protected Double convert(String in) {
		try {
			return Double.parseDouble(in);
		} catch (NumberFormatException nfe) {
			return null;
		}
	}
	
	@Override
	protected Set<String> getLegalValues() {
		return ImmutableSet.of("a floating-point number");
	}
}
