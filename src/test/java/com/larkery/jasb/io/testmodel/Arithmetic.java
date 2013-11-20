package com.larkery.jasb.io.testmodel;

import com.larkery.jasb.bind.BindNamedArgument;
import com.larkery.jasb.bind.Identity;

/**
 * 
 * (thing name:hello)
 * 
 * #hello
 * 
 */
public abstract class Arithmetic {
	public String name;

	@BindNamedArgument
	@Identity
	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}
}
