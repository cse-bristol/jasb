package com.larkery.jasb.io.impl;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.larkery.jasb.bind.Bind;
import com.larkery.jasb.bind.PointlessWrapper;
import com.larkery.jasb.io.IAtomReader;
import com.larkery.jasb.io.IReadContext;
import com.larkery.jasb.sexp.Atom;
import com.larkery.jasb.sexp.Invocation;
import com.larkery.jasb.sexp.Node;
import com.larkery.jasb.sexp.errors.IErrorHandler;

public class Reader  {
	private final Map<Class<?>, Switcher<?>> switchers = new HashMap<>();
	private final Map<Class<?>, InvocationReader<?>> specificReaders = new HashMap<>();
	private final Set<Class<?>> boundClasses;
	private final Set<? extends IAtomReader> atomReaders;
	
	
	public Reader(final Set<Class<?>> boundClasses, final Set<? extends IAtomReader> atomReaders) {
		super();
		
		final Set<Class<?>> concrete = ImmutableSet.copyOf(Collections2.filter(boundClasses, 
				new Predicate<Class<?>>() {

					@Override
					public boolean apply(final Class<?> input) {
						return !Modifier.isAbstract(input.getModifiers());
					}
				}
				));
		
		checkConsistency(concrete, atomReaders);
		
		this.boundClasses = concrete;
		this.atomReaders = atomReaders;
	}
	
	public IReadContext getContext(final IErrorHandler delegate) {
		return new Context(delegate);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static void checkConsistency(final Set<Class<?>> classes, final Set<? extends IAtomReader> atomReaders) {
		for (final Class<?> clazz : classes) {
			if (Modifier.isAbstract(clazz.getModifiers())) {
				throw new IllegalArgumentException(clazz + " cannot be unmarshalled from s-expressions as it is abstract");
			}
			if (clazz.getEnclosingClass() != null) {
				if (!Modifier.isStatic(clazz.getModifiers())) {
					throw new IllegalArgumentException(clazz + " cannot be unmarshalled from s-expressions as it is a non-static inner class");
				}
			}
			if (!(clazz.isAnnotationPresent(Bind.class) || clazz.isAnnotationPresent(PointlessWrapper.class))) {
				throw new IllegalArgumentException(clazz + " has no bind annotation");
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
					
					if (Node.class.isAssignableFrom(pd.boxedPropertyType)) {
						canRead = true;
					}
					
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
								//TODO check for stupid wrapper types here
								break;
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

	class Context implements IReadContext {
		private final IErrorHandler delegateErrorHandler;
		private final Resolver resolver = new Resolver();
		
		Context(final IErrorHandler delegateErrorHandler) {
			super();
			this.delegateErrorHandler = delegateErrorHandler;
		}

		@Override
		public void handle(final IError error) {
			delegateErrorHandler.handle(error);
		}

		@Override
		public <T> ListenableFuture<T> getCrossReference(final Class<T> clazz, final Atom where, final String identity) {
			return resolver.resolve(where, identity, clazz);
		}

		@Override
		public <T> ListenableFuture<T> read(final Class<T> clazz, final Node node) {
			if (clazz.isInstance(node)) {
				return Futures.immediateFuture(clazz.cast(node));
			} else {
				return Reader.this.getSwitcher(clazz).read(this, node);
			}
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
			//TODO Handle pointless wrapper annotation here
			if (sub.isAnnotationPresent(PointlessWrapper.class)) {
				try {
					for (final PropertyDescriptor pd : Introspector.getBeanInfo(sub).getPropertyDescriptors()) {
						if (pd.getReadMethod().isAnnotationPresent(PointlessWrapper.class)) {
							final InvocationReader<T> out = new InvocationReader<T>(sub, 
									"", new String [0]
									) {
								@Override
								protected T read(final IReadContext context, final Invocation invocation) {
									final ListenableFuture<?> read = context.read(pd.getReadMethod().getReturnType(), invocation.node);
									
									try {
										return wrap(read.get());
									} catch (InterruptedException | ExecutionException e) {
										throw new RuntimeException("could not read invocation as required type", e);
									}
								}
	
								private T wrap(final Object read) {
									try {
										final T result = sub.getConstructor().newInstance();
										
										pd.getWriteMethod().invoke(result, read);
										
										return result;
									} catch (InstantiationException
											| IllegalAccessException
											| IllegalArgumentException
											| InvocationTargetException
											| NoSuchMethodException
											| SecurityException e) {
										throw new RuntimeException("Bad pointless-wrapper " + sub, e);
									}
								}
							};
							
							specificReaders.put(sub, out);
							break;
						}
					}
				} catch (final Exception e) {
					throw new RuntimeException("bad pointless-wrapper " + sub + "(" + e.getMessage() + ")", e);
				}
			} else {
				final InvocationReader<T> reader = new InvocationReaderLoader<T>(sub).getReaderInstance();
				specificReaders.put(sub, reader);
			}
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
}
