package com.larkery.jasb.bind.impl;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.TypeToken;
import com.larkery.jasb.bind.Bind;
import com.larkery.jasb.bind.BindNamedArgument;
import com.larkery.jasb.bind.BindPositionalArgument;
import com.larkery.jasb.bind.BindRemainingArguments;
import com.larkery.jasb.bind.id.CreatesReferenceScope;
import com.larkery.jasb.bind.id.Identity;
import com.larkery.jasb.sexp.Atom;
import com.larkery.jasb.sexp.BasicError;
import com.larkery.jasb.sexp.IErrorHandler;
import com.larkery.jasb.sexp.Invocation;
import com.larkery.jasb.sexp.Node;

/**
 * Represents a class with {@link Bind} on it; handles constructing the class
 * from a {@link Node}.
 * 
 * @author hinton
 * 
 * @param <T>
 */
class ObjectMapping<T> {
	private static final Logger log = LoggerFactory.getLogger(ObjectMapping.class);
	/**
	 * The name in the {@link Bind} on {@link #boundType}
	 */
	private final String boundName;
	/**
	 * The class that is bound
	 */
	final Class<T> boundType;

	private final List<MethodMapping<T, ?>> methods;
	
	/**
	 * Invoked after binding with information about the position and so on
	 */
	private final Optional<Method> afterBinding;
	/**
	 * The noarg constructor for {@link #boundType}
	 */
	private final Constructor<T> constructor;
	private final Optional<String> identifierKey;
	private final Optional<CreatesReferenceScope> topScope;
	
	final IErrorHandler errors;
	final Binder binder;

	public static <A> ObjectMapping<A> of (final IErrorHandler errors, final Binder result, final Class<A> c) {
		return new ObjectMapping<>(errors, result, c);
	}
	
	ObjectMapping(final IErrorHandler errors, final Binder binder, final Class<T> clazz) {
		this.errors = errors;
		this.binder = binder;
		this.boundType = clazz;
		
		this.boundName = clazz.getAnnotation(Bind.class).value();
		try {
			this.constructor = boundType.getConstructor();
		} catch (NoSuchMethodException | SecurityException e1) {
			throw new RuntimeException(boundType.getCanonicalName()
					+ " lacks an accessible noargs constructor");
		}
		
		List<MethodMapping<T, ?>> methods = new ArrayList<>();
		
		final AtomicInteger pc = new AtomicInteger();
		
		Method identifier = null;
		
		for (final Method method : clazz.getMethods()) {
			if (isBindingMethod(method)) {
				final MethodMapping<T, ?> mapping = new MethodMapping<T, Object>(pc, this, method);
				methods.add(mapping);
			} else if (isAfterMethod(method)) {
				
			}
			
			if (isIdentifierMethod(method)) {
				identifier = method;
			}
		}
		
		afterBinding = Optional.absent();
		
		if (identifier != null) {
			identifierKey = Optional.of(identifier.getAnnotation(BindNamedArgument.class).value());
		} else {
			identifierKey = Optional.absent();
		}
		
		topScope = Optional.fromNullable(clazz.getAnnotation(CreatesReferenceScope.class));
		
		// TODO we need to sort methods so that they are considered in the correct order
		this.methods = ImmutableList.copyOf(methods);
	}
	
	private boolean isIdentifierMethod(Method method) {
		return method.isAnnotationPresent(Identity.class);
	}

	private boolean isAfterMethod(Method method) {
		return method.isAnnotationPresent(BindRemainingArguments.class);
	}

	private static boolean isBindingMethod(final Method method) {
		return method.isAnnotationPresent(BindNamedArgument.class) ||
				method.isAnnotationPresent(BindPositionalArgument.class) ||
				method.isAnnotationPresent(BindRemainingArguments.class);
	}
	
	public T construct(final Invocation invocation) {
		T result;
		try {
			result = constructor.newInstance();
			
			if (identifierKey.isPresent()) {
				final Node identifier = invocation.arguments.get(identifierKey.get());
				if (identifier != null) {
					if (identifier instanceof Atom) {
						binder.resolver.define(((Atom) identifier).getValue(), TypeToken.of(boundType), result);
					} else {
						errors.handle(BasicError.at(identifier, "expected a string identifier"));
					}
				}
			}
			
			int counter = 0;
			try {
				if (topScope.isPresent()) {
					binder.resolver.pushBlock(ImmutableSet.copyOf(topScope.get().holds()));
					counter++;
				}
				
				for (final MethodMapping<T, ?> method : methods) {
					if (method.isBlockDefinition()) {
						binder.resolver.pushBlock(method.getBlockClasses());
						counter++;	
					}
					
					method.populate(invocation, result);
				}
			} finally {
				while (counter > 0) {
					counter--;
					binder.resolver.popBlock();
				}
			}
		} catch (InstantiationException | IllegalAccessException
				| IllegalArgumentException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}

		return result;
	}

	public TypeToken<?> getBoundType() {
		return TypeToken.of(boundType);
	}

	public String getName() {
		return boundName;
	}
}