package com.larkery.jasb.sexp.parse;

import java.util.Stack;

import com.google.common.base.Optional;
import com.larkery.jasb.sexp.ISexpVisitor;
import com.larkery.jasb.sexp.Location;

abstract class Cutout<Q extends ISexpVisitor> implements ISexpVisitor {
	private final Stack<ISexpVisitor> cutouts = new Stack<>();
	private int depth = 0;
	private final Stack<Integer> depths = new Stack<>();
	
	private boolean afterOpen = false;
	private Location openLocation;
	private Location location;
	
	protected Cutout(final ISexpVisitor delegate) {
		super();
		cutouts.push(delegate);
		depths.push(0);
	}

	protected abstract Optional<Q> cut(final String head);
	protected abstract void paste(final Q q);
	
	public void locate(Location loc) {
		this.location = loc;
	}

	private void shiftOpen() {
		if (afterOpen) {
			cutouts.peek().locate(openLocation);
			cutouts.peek().open();
			depth++;
			afterOpen = false;
		}
	}
	
	public void open() {
		shiftOpen();
		afterOpen = true;
		openLocation = location;
	}

	public void atom(final String string) {
		if (afterOpen) {
			final Optional<Q> cutter = cut(string);
			
			if (cutter.isPresent()) {
				cutouts.push(cutter.get());
				depths.push(depth);
			}
			
			shiftOpen();
		}
		
		cutouts.peek().locate(location);
		cutouts.peek().atom(string);
	}

	@SuppressWarnings("unchecked")
	public void close() {
		shiftOpen();
		cutouts.peek().close();
		depth--;
		if (depth <= depths.peek() && cutouts.size() > 1) {
			final Q cutter = (Q) cutouts.pop();
			paste(cutter);
			depths.pop();
		}
	}
}
