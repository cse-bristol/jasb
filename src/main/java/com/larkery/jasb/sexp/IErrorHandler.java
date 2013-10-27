package com.larkery.jasb.sexp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface IErrorHandler {
	static final Logger log = LoggerFactory.getLogger(IErrorHandler.class);
	public static final IErrorHandler SLF4J = new IErrorHandler() {
		@Override
		public void warning(Location location, String message) {
			log.warn("{}: {}", location, message);
		}
		
		@Override
		public void error(Location location, String message) {
			log.error("{}: {}", location, message);
		}
	};
	
	public void error(final Location location, final String message);
	public void warning(final Location location, final String message);
}
