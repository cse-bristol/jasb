package com.larkery.jasb.io.impl;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.larkery.jasb.bind.Bind;
import com.larkery.jasb.bind.id.IResolver;
import com.larkery.jasb.bind.id.Resolver;
import com.larkery.jasb.io.IAtomReader;
import com.larkery.jasb.io.IReadContext;
import com.larkery.jasb.sexp.Atom;
import com.larkery.jasb.sexp.Node;
import com.larkery.jasb.sexp.errors.IErrorHandler;

public class ReadContext implements IReadContext {
	private final Map<Class<?>, Switcher<?>> switchers = new HashMap<>();
	private final Map<Class<?>, InvocationReader<?>> specificReaders = new HashMap<>();
	private final Set<Class<?>> boundClasses;
	private final Set<IAtomReader> atomReaders;
	private final IResolver resolver = new Resolver();
	
	public ReadContext(final Set<Class<?>> boundClasses, final Set<IAtomReader> atomReaders) {
		super();
		checkConsistency(boundClasses, atomReaders);

		this.boundClasses = boundClasses;
		this.atomReaders = atomReaders;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static void checkConsistency(final Set<Class<?>> classes, final Set<IAtomReader> atomReaders) {
		for (final Class<?> clazz : classes) {
			if (!clazz.isAnnotationPresent(Bind.class)) {
				throw new IllegalArgumentException(clazz + " has no bind annotation");
			}
			if (Modifier.isAbstract(clazz.getModifiers())) {
				throw new IllegalArgumentException(clazz + " cannot be unmarshalled from s-expressions as it is abstract");
			}
			if (clazz.getEnclosingClass() != null) {
				if (!Modifier.isStatic(clazz.getModifiers())) {
					throw new IllegalArgumentException(clazz + " cannot be unmarshalled from s-expressions as it is a non-static inner class");
				}
			}
			try {
				final Constructor<?> constructor = clazz.getConstructor();
				
				final Object o;
				try {
					o = constructor.newInstance();
				} catch (InstantiationException | IllegalAccessException
						| IllegalArgumentException | InvocationTargetException e) {
					throw new IllegalArgumentException("Constructing " + clazz
							+ " causes an error", e);
				}
					
				final Set<JasbPropertyDescriptor> descriptors = JasbPropertyDescriptor.getDescriptors(clazz);
				for (final JasbPropertyDescriptor pd : descriptors) {
					if (pd.isMultiple) {
						Object initialValue;
						try {
							initialValue = pd.readMethod.invoke(o);
						} catch (IllegalAccessException
								| IllegalArgumentException
								| InvocationTargetException e) {
							throw new IllegalArgumentException("Invoking the read method for " + pd + " on clazz caused an error", e);
						}
						if (initialValue instanceof List) {
							try {
								((List) initialValue).add(null);
							} catch (final Throwable th) {
								throw new IllegalArgumentException("Had an error adding a null to the list produced by " + pd + " in " + clazz, th);
							}
						} else {
							throw new IllegalArgumentException(pd + " in " + clazz + " does not produce a list, but " + initialValue);
						}
					}
					
					boolean canRead = false;
					for (final IAtomReader reader : atomReaders) {
						if (reader.canReadTo(pd.boxedPropertyType)) {
							canRead = true;
							break;
						}
					}
					
					if (!canRead && pd.boxedPropertyType.isAnnotationPresent(Bind.class)) {
						canRead = true;
					}
					
					if (!canRead) {
						for (final Class<?> possible : classes) {
							if (pd.boxedPropertyType.isAssignableFrom(possible)) {
								canRead = true;
							}
						}
					}
					
					if (!canRead) {
						throw new IllegalArgumentException(pd + " in " + clazz + 
								" has no legal values in the set of input classes or supported atom types");
					}
				}
				
				
			} catch (NoSuchMethodException | SecurityException e) {
				throw new IllegalArgumentException(clazz + " does not have an accessible no-args constructor");
			}
		}
	}

	@Override
	public void handle(final IError error) {
		IErrorHandler.SLF4J.handle(error);
	}

	@Override
	public <T> ListenableFuture<T> getCrossReference(final Class<T> clazz, final Atom atom, final String id) {
		return resolver.resolve(atom, id, clazz);
	}

	@Override
	public <T> ListenableFuture<T> read(final Class<T> clazz, final Node node) {
		return getSwitcher(clazz).read(this, node);
	}

	private <T> Switcher<T> getSwitcher(final Class<T> clazz) {
		@SuppressWarnings("unchecked")
		Switcher<T> out = (Switcher<T>) switchers.get(clazz);
		
		if (out == null) {
			out = new Switcher<>(clazz, createReaders(clazz), createAtomReader(clazz));
			switchers.put(clazz, out);
		}
		
		return out;
	}
	
	@SuppressWarnings("unchecked")
	private <T> Set<InvocationReader<? extends T>> createReaders(final Class<T> clazz) {
		final ImmutableSet.Builder<InvocationReader<? extends T>> builder = ImmutableSet.builder();
		for (final Class<?> sub : boundClasses) {
			if (clazz.isAssignableFrom(sub)) {
				//TODO check not abstract etc
				builder.add((InvocationReader<? extends T>) getOrCreateInvocationReader(sub));
			}
		}
		return builder.build();
	}
	
	@SuppressWarnings("unchecked")
	private <T> InvocationReader<T> getOrCreateInvocationReader(final Class<T> sub) {
		if (!specificReaders.containsKey(sub)) {
			final InvocationReader<T> reader = new InvocationReaderLoader<T>(sub).getReaderInstance();
			specificReaders.put(sub, reader);
		}
		return (InvocationReader<T>) specificReaders.get(sub);
	}

	private <T> MultiAtomReader<T> createAtomReader(final Class<T> clazz) {
		final ImmutableSet.Builder<IAtomReader> readers = ImmutableSet.builder();
		for (final IAtomReader reader : atomReaders) {
			if (reader.canReadTo(clazz)) {
				readers.add(reader);
			}
		}
		return new MultiAtomReader<T>(clazz, readers.build());
	}
	
	@Override
	public <T> ListenableFuture<List<T>> readMany(final Class<T> clazz, final Iterable<Node> nodes) {
		final ImmutableList.Builder<ListenableFuture<T>> futures = ImmutableList.builder();
		for (final Node node : nodes) {
			futures.add(read(clazz, node));
		}
		return Futures.allAsList(futures.build());
	}
	
	@Override
	public void registerIdentity(final Object o, final ListenableFuture<String> future) {
		Futures.addCallback(future, new FutureCallback<String>() {
			@Override
			public void onSuccess(final String result) {
				resolver.define(result, o);
			}
			
			@Override
			public void onFailure(final Throwable t) {}
		});
	}
}
