package com.larkery.jasb.io;

import java.lang.reflect.Method;
import java.util.Set;

import com.google.common.base.Optional;

public interface IModel {
	public Set<IElement> getElements();
	public Set<IInvocationModel> getInvocations();
	public Set<IAtomModel> getAtoms();
	
	public interface IElement extends Comparable<IElement> {
		public String getName();
		public Class<?> getJavaType();
	}
	
	public interface IInvocationModel extends IElement {
		public Set<IArgument> getArguments();
		public Set<IArgument> getNamedArguments();
		public Set<IArgument> getPositionalArguments();
		public Optional<IArgument> getRemainderArgument();
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
		public Method getReadMethod();
		
		public Set<IElement> getLegalValues();
	}
}
