package com.larkery.jasb.io.impl;

import java.util.HashSet;
import java.util.Set;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.larkery.jasb.bind.Bind;
import com.larkery.jasb.io.IAtomReader;
import com.larkery.jasb.io.IModel;

public class Model implements IModel {
	private final ImmutableSet<IElement> elements;

	public Model(final Set<Class<?>> classes, final Set<? extends IAtomReader> atoms) {
		final ImmutableSet.Builder<IElement> elements = ImmutableSet.builder();
		
		final Set<Class<?>> atomTypes = new HashSet<>();
		for (final Class<?> clazz : classes) {
			final InvocationModel inv = new InvocationModel(clazz);
			elements.add(inv);
			for (final IArgument a : inv.getArguments()) {
				if (atomTypes.contains(a.getJavaType())) continue;
				atomTypes.add(a.getJavaType());
				for (final IAtomReader r : atoms) {
					if (r.canReadTo(a.getJavaType())) {
						final AtomModel am = new AtomModel(
								a.getJavaType(),
								r.getLegalValues(a.getJavaType())
								);
						elements.add(am);
					}
				}
			}
		}
		this.elements = elements.build();
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

		public InvocationModel(final Class<?> clazz) {
			this.javaType = clazz;
			this.name = clazz.getAnnotation(Bind.class).value();
			
			final ImmutableSet.Builder<IArgument> arguments = 
					ImmutableSet.builder();
			for (final JasbPropertyDescriptor pd : JasbPropertyDescriptor.getDescriptors(javaType)) {
				arguments.add(new Argument(pd));
			}
			this.arguments = arguments.build();
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
		public String toString() {
			return "invocation " + name;
		}
	}
	
	static class AtomModel implements IAtomModel {
		private final Class<?> javaType;
		private final ImmutableSet<String> legalValues;

		public AtomModel(final Class<?> javaType, final Set<String> legalValues) {
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
	}
}
