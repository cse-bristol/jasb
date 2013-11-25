package com.larkery.jasb.io;

import java.util.Set;

import com.google.common.base.Optional;

public interface IModel {
	public Set<IElement> getElements();
	
	public interface IElement {
		public Class<?> getJavaType();
	}
	
	public interface IInvocationModel extends IElement {
		public String getName();
		public Set<IArgument> getArguments();
	}
	
	public interface IAtomModel extends IElement {
		public Set<String> getLiterals();
	}
	
	public interface IArgument {
		public boolean isNamedArgument();
		public Optional<String> getName();
		public boolean isPositionalArgument();
		public Optional<Integer> getPosition();
		public boolean isRemainderArgument();
		public Class<?> getJavaType();
		public boolean isMultiple();
		
		public Set<IElement> getLegalValues();
	}
}
