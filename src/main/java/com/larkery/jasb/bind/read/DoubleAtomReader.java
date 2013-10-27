package com.larkery.jasb.bind.read;


public class DoubleAtomReader extends SimpleAtomReader<Double> {
	@Override
	protected Double convert(String in) {
		try {
			return Double.parseDouble(in);
		} catch (NumberFormatException nfe) {
			return null;
		}
	}
}
