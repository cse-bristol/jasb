package com.larkery.jasb.bind;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.larkery.jasb.sexp.IErrorHandler;
import com.larkery.jasb.sexp.Invocation;
import com.larkery.jasb.sexp.Node;
import com.larkery.jasb.sexp.Seq;

class BoundType<T> {
	private final String boundName;
	private final Class<T> boundType;
	
	private final Map<String, BoundArgument<?>> namedArguments;
	private final List<BoundArgument<?>> sequencedArguments;
	private final BoundArgument<?> remainder;
	private final Method afterBinding;
	private final Constructor<T> constructor;
	
	class BoundArgument<Q> {
		private final Method setter;
		private final Class<Q> setterType;
		private final boolean multiValue;
		
		@SuppressWarnings("unchecked")
		public BoundArgument(final Method setter) {
			this.setter = setter;
			
			// handle list-valued parameters nicely here
			
		}

		public void populate(Binder binder, T result, Node value, IErrorHandler errors) {
			if (multiValue) {
				if (value instanceof Seq) {
					populateList(binder, result, ((Seq)value).getNodes(), errors);
				} else {
					populateList(binder, result, ImmutableList.of(value), errors);
				}
			} else {
				final Q transformed = binder.read(value, setterType, errors);
				try {
					setter.invoke(result, transformed);
				} catch (IllegalAccessException | IllegalArgumentException
						| InvocationTargetException e) {
					throw new RuntimeException(e);
				}
			}
		}

		public void populateList(Binder binder, T result, List<Node> remainder,IErrorHandler errors) {
			// convert each node and that.
		}
	}
	
	public T construct(final Binder binder, final Invocation invocation, final IErrorHandler errors) {
		T result;
		try {
			result = constructor.newInstance();
			populate(binder, result, invocation, errors);
		} catch (InstantiationException | IllegalAccessException
				| IllegalArgumentException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
		
		return result;
	}
	
	private void populate(final Binder binder, T result, Invocation invocation, IErrorHandler errors) {
		for (final Map.Entry<String, Node> arg : invocation.arguments.entrySet()) {
			if (namedArguments.containsKey(arg.getKey())) {
				namedArguments.get(arg.getKey()).populate(binder, result, arg.getValue(), errors);
			} else {
				errors.error(arg.getValue().getLocation(), 
						"unexpected argument " + arg.getKey() + " to " + boundName);
			}
		}
		
		final List<Node> indexed = invocation.remainder.subList(0, 
				Math.min(invocation.remainder.size(),
						sequencedArguments.size()));
		
		{
			final Iterator<Node> ni = indexed.iterator();
			final Iterator<BoundArgument<?>> ai = sequencedArguments.iterator();
			while (ni.hasNext() && ai.hasNext()) {
				final Node node = ni.next();
				final BoundArgument<?> arg = ai.next();
				arg.populate(binder, result, node, errors);
			}
		}
		
		if (invocation.remainder.size() > sequencedArguments.size()) {
			final List<Node> remainder = invocation.remainder.subList(sequencedArguments.size(), invocation.remainder.size());
			if (this.remainder != null) {
				this.remainder.populateList(binder, result, 
						remainder
						, errors);
			} else {
				errors.error(remainder.get(0).getLocation(), "unused additional arguments to " + boundName);
			}
		}
	}

	@SuppressWarnings("unchecked")
	public BoundType(final Class<T> clazz) {
		this.boundType = clazz;
		this.boundName = clazz.getAnnotation(BindElement.class).value();
		try {
			this.constructor = boundType.getConstructor();
		} catch (NoSuchMethodException | SecurityException e1) {
			throw new RuntimeException(boundType.getCanonicalName() + " lacks an accessible noargs constructor");
		}
		final ImmutableMap.Builder<String, BoundArgument<?>> args = ImmutableMap.builder();
		final ImmutableMap.Builder<Integer, BoundArgument<?>> posn = ImmutableMap.builder();
		
		Method remainder = null;
		Method after = null;
		
		for (final Method m : clazz.getMethods()) {
			if (Modifier.isStatic(m.getModifiers())) continue;
			if (!Modifier.isPublic(m.getModifiers())) continue;
			
			if (m.getAnnotation(BindNamedArgument.class) != null) {
				args.put(m.getAnnotation(BindNamedArgument.class).value(), new BoundArgument<>(setter(clazz, m)));
			} else if (m.getAnnotation(BindPositionalArgument.class) != null) {
				posn.put(m.getAnnotation(BindPositionalArgument.class).position(), 
						new BoundArgument<>(setter(clazz, m)));
			} else if (m.getAnnotation(BindRemainingArguments.class) != null) {
				remainder = setter(clazz, m);
			} else if (m.getAnnotation(AfterBinding.class) != null) {
				after = m;
			}
		}
		
		this.afterBinding = after;
		this.remainder = remainder == null ? null : new BoundArgument(remainder);
		this.namedArguments = args.build();
		
		ImmutableMap<Integer, BoundArgument<?>> build = posn.build();
		ImmutableList.Builder<BoundArgument<?>> positional = ImmutableList.builder();
		int counter = 0;
		for (final Map.Entry<Integer, BoundArgument<?>> e : new TreeMap<>(build).entrySet()) {
			if (e.getKey() != counter) {
				throw new RuntimeException("Positional arguments must be sequential and start from zero; " + e + " is problematic");
			}
			positional.add(e.getValue());
		}
		this.sequencedArguments = positional.build();
	}

	private static Method setter(Class<?> clazz, Method m) {
		if (m.getReturnType() == Void.TYPE) {
			throw new RuntimeException(String.format("%s.%s is void (should be a getter)", clazz.getSimpleName(), m.getName()));
		}
		
		if (m.getParameterTypes().length != 0) {
			throw new RuntimeException(String.format("%s.%s has parameters (should be a getter)", clazz.getSimpleName(), m.getName()));
		}
			
		try {
			if (m.getName().startsWith("is")) {
				return clazz.getMethod("set"+m.getName().substring(2), m.getReturnType());	
			} else if (m.getName().startsWith("get")) {
				return clazz.getMethod("set"+m.getName().substring(3), m.getReturnType());
			} else {
				throw new RuntimeException(String.format("%s.%s doesn't start with is or get", clazz.getSimpleName(), m.getName()));
			}
		} catch (NoSuchMethodException | SecurityException e) {
			throw new RuntimeException(String.format("%s.%s doesn't have an accessible setter", clazz.getSimpleName(), m.getName()));
		}
	}
}
