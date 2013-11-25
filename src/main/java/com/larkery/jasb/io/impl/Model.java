package com.larkery.jasb.io.impl;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.larkery.jasb.bind.Bind;
import com.larkery.jasb.io.IAtomReader;
import com.larkery.jasb.io.IModel;

public class Model implements IModel {
	private final ImmutableSet<IElement> elements;
	private final ImmutableSet<IInvocationModel> invocations;
	private final ImmutableSet<IAtomModel> atoms;
	
	public Model(final Set<Class<?>> classes, final Set<? extends IAtomReader> atoms) {
		final ImmutableSet.Builder<IElement> elements = ImmutableSet.builder();
		final ImmutableSet.Builder<IAtomModel> atomModels = ImmutableSet.builder();
		final ImmutableSet.Builder<IInvocationModel> invocations = ImmutableSet.builder();
		
		final Set<Class<?>> atomTypes = new HashSet<>();
		for (final Class<?> clazz : classes) {
			final InvocationModel inv = new InvocationModel(clazz);
			elements.add(inv);
			invocations.add(inv);
			for (final IArgument a : inv.getArguments()) {
				if (atomTypes.contains(a.getJavaType())) continue;
				atomTypes.add(a.getJavaType());
				for (final IAtomReader r : atoms) {
					if (r.canReadTo(a.getJavaType())) {
						final AtomModel am = new AtomModel(
								r.getDisplayName(a.getJavaType()),
								a.getJavaType(),
								r.getLegalValues(a.getJavaType())
								);
						elements.add(am);
						atomModels.add(am);
					}
				}
			}
		}
		this.elements = elements.build();
		this.atoms = atomModels.build();
		this.invocations = invocations.build();
	}
	
	@Override
	public Set<IAtomModel> getAtoms() {
		return atoms;
	}
	
	@Override
	public Set<IInvocationModel> getInvocations() {
		return invocations;
	}
	
	@Override
	public Set<IElement> getElements() {
		return elements;
	}
	
	class Argument implements IArgument {
		private final JasbPropertyDescriptor pd;
		private Set<IElement> legalValues;

		public Argument(final JasbPropertyDescriptor pd) {
			this.pd = pd;
		}

		@Override
		public boolean isNamedArgument() {
			return pd.key.isPresent();
		}

		@Override
		public Optional<String> getName() {
			return pd.key;
		}

		@Override
		public boolean isPositionalArgument() {
			return pd.position.isPresent();
		}

		@Override
		public Optional<Integer> getPosition() {
			return pd.position;
		}

		@Override
		public boolean isMultiple() {
			return pd.isMultiple;
		}
		
		@Override
		public boolean isRemainderArgument() {
			return !(isPositionalArgument() || isNamedArgument());
		}
		
		@Override
		public Method getReadMethod() {
			return pd.readMethod;
		}

		@Override
		public Set<IElement> getLegalValues() {
			if (legalValues == null) {
				final ImmutableSet.Builder<IElement> legalValues = ImmutableSet.builder();
				
				for (final IElement e : Model.this.elements) {
					if (pd.boxedPropertyType.isAssignableFrom(e.getJavaType())) {
						legalValues.add(e);
					}
				}
				
				this.legalValues = legalValues.build();
			}
			return legalValues;
		}
		
		@Override
		public Class<?> getJavaType() {
			return pd.boxedPropertyType;
		}
	}
	
	class InvocationModel implements IInvocationModel {
		private final Class<?> javaType;
		private final ImmutableSet<IArgument> arguments;
		private final String name;
		private final ImmutableSet<IArgument> named;
		private final ImmutableSet<IArgument> positional;
		private final Optional<IArgument> remainder;

		public InvocationModel(final Class<?> clazz) {
			this.javaType = clazz;
			if (!clazz.isAnnotationPresent(Bind.class)) {
				throw new IllegalArgumentException(""+clazz);
			}
			this.name = clazz.getAnnotation(Bind.class).value();
			
			final ImmutableSet.Builder<IArgument> arguments = 
					ImmutableSet.builder();
			
			final ImmutableSet.Builder<IArgument> named = 
					ImmutableSet.builder();
			
			final ImmutableSet.Builder<IArgument> positional = 
					ImmutableSet.builder();
			
			IArgument remainder = null;
			for (final JasbPropertyDescriptor pd : JasbPropertyDescriptor.getDescriptors(javaType)) {
				final Argument argument = new Argument(pd);
				arguments.add(argument);
				if (argument.isNamedArgument()) named.add(argument);
				if (argument.isPositionalArgument()) positional.add(argument);
				if (argument.isRemainderArgument()) remainder = argument;
			}
			
			this.arguments = arguments.build();
			this.named = named.build();
			this.positional = positional.build();
			this.remainder = Optional.fromNullable(remainder);
		}

		@Override
		public Class<?> getJavaType() {
			return this.javaType;
		}

		@Override
		public String getName() {
			return this.name;
		}

		@Override
		public Set<IArgument> getArguments() {
			return this.arguments;
		}
		
		@Override
		public Set<IArgument> getNamedArguments() {
			return named;
		}
		
		@Override
		public Set<IArgument> getPositionalArguments() {
			return positional;
		}
		
		@Override
		public Optional<IArgument> getRemainderArgument() {
			return remainder;
		}
		
		@Override
		public String toString() {
			return "invocation " + name;
		}
		
		@Override
		public int compareTo(final IElement o) {
			if (o instanceof IInvocationModel) {
				return name.compareTo(((IInvocationModel) o).getName());
			} else {
				return -1;
			}
		}
	}
	
	static class AtomModel implements IAtomModel {
		private final String name;
		private final Class<?> javaType;
		private final ImmutableSet<String> legalValues;

		public AtomModel(final String name, final Class<?> javaType, final Set<String> legalValues) {
			this.name = name;
			this.javaType = javaType;
			this.legalValues = ImmutableSet.copyOf(legalValues);
		}

		@Override
		public Class<?> getJavaType() {
			return javaType;
		}

		@Override
		public Set<String> getLiterals() {
			return legalValues;
		}
		
		@Override
		public String toString() {
			return "atom " + legalValues;
		}
		
		@Override
		public String getName() {
			return name;
		}
		
		@Override
		public int compareTo(final IElement o) {
			if (o instanceof IAtomModel) {
				return name.compareTo(o.getName());
			} else {
				return 1;
			}
		}
	}
}
