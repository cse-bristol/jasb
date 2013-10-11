package com.larkery.jasb;

public interface ISJavaTypeAdapter<From, To> {
	public Class<From> getFrom();
	public Class<To> getTo();
	public To read(final From from);
	public From write(final To to);
}
