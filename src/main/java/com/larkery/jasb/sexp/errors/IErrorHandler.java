package com.larkery.jasb.sexp.errors;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.larkery.jasb.sexp.Location;
import com.larkery.jasb.sexp.Node;

public interface IErrorHandler {
	public interface IError {
		public Set<Location> getLocations();
		public Set<Node> getNodes();
		public String getMessage();
		public Type getType();
		public enum Type {
			ERROR,
			WARNING
		}
	}
	
	static final Logger log = LoggerFactory.getLogger(IErrorHandler.class);
	public static final IErrorHandler SLF4J = new BaseErrorHandler() {
		@Override
		public void handle(final IError error) {
			switch (error.getType()) {
			case ERROR:
				log.error("{} : {}", error.getLocations(), error.getMessage());
				break;
			case WARNING:
				log.warn("{} : {}", error.getLocations(), error.getMessage());
				break;			
			}
		}
	};

	public static final IErrorHandler NOP = new BaseErrorHandler() {
		@Override
		public void handle(final IError error) {}
	};
	static final IErrorHandler RAISE = new BaseErrorHandler(){
	
		@Override
		public void handle(final IError error) {
			throw new JasbErrorException(error);
		}
	};
	static final IErrorHandler IGNORE = new BaseErrorHandler() {
		@Override public void handle(final IError error) {}
	};
	
	public void handle(final IError error);
	
	public void handle(final Location location, final String format, final Object... interpolate);
	public void handle(final Node location, final String format, final Object... interpolate);
}
