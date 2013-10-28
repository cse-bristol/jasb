package com.larkery.jasb.bind.impl;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.TypeToken;
import com.google.common.util.concurrent.FutureCallback;
import com.larkery.jasb.bind.BindNamedArgument;
import com.larkery.jasb.bind.BindPositionalArgument;
import com.larkery.jasb.bind.BindRemainingArguments;
import com.larkery.jasb.bind.id.CreatesReferenceScope;
import com.larkery.jasb.sexp.Invocation;
import com.larkery.jasb.sexp.Node;
import com.larkery.jasb.sexp.Seq;

public class MethodMapping<Target, Val> {
	private static final Type LIST_TYPE_ARGUMENT;
	static {
		try {
			LIST_TYPE_ARGUMENT = List.class.getMethod("get", int.class)
					.getGenericReturnType();
		} catch (NoSuchMethodException | SecurityException e) {
			throw new RuntimeException(
					"Can't see List.get - this should really never happen.");
		}
	}                                                                                           
	
	private final Optional<String> name;
	private final Optional<Integer> position;
	private final Optional<CreatesReferenceScope> blockDefinition;
	private AtomicInteger positionalCount;
	private final TypeToken<Val> valueType;
	private final boolean multiValue;
	private final Method method;
	private ObjectMapping<Target> objectMapping;
	
	@SuppressWarnings("unchecked")
	public MethodMapping(final AtomicInteger positionalCount, final ObjectMapping<Target> objectMapping, Method method) {
		this.positionalCount = positionalCount;
		this.objectMapping = objectMapping;
		
		if (method.isAnnotationPresent(BindNamedArgument.class)) {
			final BindNamedArgument named = method.getAnnotation(BindNamedArgument.class);
			this.name = Optional.of(named.value());
			this.position = Optional.absent();
		} else if (method.isAnnotationPresent(BindPositionalArgument.class)) {
			final BindPositionalArgument positional = method.getAnnotation(BindPositionalArgument.class);
			this.position = Optional.of(positional.position());
			positionalCount.incrementAndGet();
			this.name = Optional.absent();
		} else if (method.isAnnotationPresent(BindRemainingArguments.class)) {
			this.name = Optional.absent();
			this.position = Optional.absent();
		} else {
			throw new RuntimeException(method.getName() + " does not bind an argument");
		}
		
		this.blockDefinition = Optional.fromNullable(method.getAnnotation(CreatesReferenceScope.class));
		
		final TypeToken<?> valueType = TypeToken.of(method.getGenericReturnType());
		if (TypeToken.of(List.class).isAssignableFrom(valueType)) {
			this.valueType = (TypeToken<Val>) valueType.resolveType(LIST_TYPE_ARGUMENT);
			this.multiValue = true;
			this.method = method;
		} else {
			this.valueType = (TypeToken<Val>) valueType;
			this.multiValue = false;
			this.method = setter(objectMapping.boundType, method);
		}
	}

	public boolean isBlockDefinition() {
		return blockDefinition.isPresent();
	}

	public Set<Class<?>> getBlockClasses() {
		if (isBlockDefinition()) {
			return ImmutableSet.copyOf(blockDefinition.get().holds());
		} else {
			return ImmutableSet.of();
		}
	}

	public void populate(final Invocation in, final Target out) {
		if (name.isPresent()) {
			final Node value = in.arguments.get(name.get());
			if (value != null) {
				setFromNode(value, out);
			}
		} else if (position.isPresent()) {
			if (in.remainder.size() > position.get()) {
				final Node value = in.remainder.get(position.get());
				setFromNode(value, out);
			}
		} else {
			final int offset = positionalCount.get();
			// process sublist
			if (in.remainder.size() > offset) {
				setFromNodes(in.remainder.subList(offset, in.remainder.size()), out);
			}
		}
	}
	
	private void setFromNodes(final List<Node> subList, Target out) {
		try {
			@SuppressWarnings("unchecked")
			final List<Val> current = (List<Val>) method.invoke(out);
			for (final Node in : subList) {
				final FutureCallback<Val> insert = Callback.insert(current, current.size());
				current.add(null);
				objectMapping.binder.read(in, valueType, insert);
			}
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	private void setFromNode(final Node value, Target out) {
		if (multiValue) {
			if (value instanceof Seq) {
				setFromNodes(((Seq) value).getNodes(), out);
			} else {
				setFromNodes(ImmutableList.of(value), out);
			}
		} else {
			objectMapping.binder.read(value, valueType, Callback.<Val>set(out, method));
		}
	}

	private static Method setter(Class<?> clazz, Method m) {
		if (m.getReturnType() == Void.TYPE) {
			throw new RuntimeException(String.format(
					"%s.%s is void (should be a getter)",
					clazz.getSimpleName(), m.getName()));
		}

		if (m.getParameterTypes().length != 0) {
			throw new RuntimeException(String.format(
					"%s.%s has parameters (should be a getter)",
					clazz.getSimpleName(), m.getName()));
		}

		try {
			if (m.getName().startsWith("is")) {
				return clazz.getMethod("set" + m.getName().substring(2),
						m.getReturnType());
			} else if (m.getName().startsWith("get")) {
				return clazz.getMethod("set" + m.getName().substring(3),
						m.getReturnType());
			} else {
				throw new RuntimeException(String.format(
						"%s.%s doesn't start with is or get",
						clazz.getSimpleName(), m.getName()));
			}
		} catch (NoSuchMethodException | SecurityException e) {
			throw new RuntimeException(String.format(
					"%s.%s doesn't have an accessible setter",
					clazz.getSimpleName(), m.getName()));
		}
	}
}
