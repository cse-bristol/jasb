package com.larkery.jasb.bind.read;


public class StringAtomReader extends SimpleAtomReader<String> {
	@Override
	protected String convert(String in) {
		return in;
	}
}
