package com.larkery.jasb.sexp;

public interface IErrorHandler {
	public static final IErrorHandler STDERR = new IErrorHandler() {
		
		@Override
		public void warning(Location location, String message) {
			System.err.println(String.format("WARN %s: %s", location, message));
		}
		
		@Override
		public void error(Location location, String message) {
			System.err.println(String.format(" ERR %s: %s", location, message));
		}
	};
	
	public void error(final Location location, final String message);
	public void warning(final Location location, final String message);
}
