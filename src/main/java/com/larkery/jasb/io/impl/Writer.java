package com.larkery.jasb.io.impl;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.larkery.jasb.bind.Bind;
import com.larkery.jasb.bind.PointlessWrapper;
import com.larkery.jasb.io.IAtomWriter;
import com.larkery.jasb.io.impl.JasbPropertyDescriptor.BoundTo;
import com.larkery.jasb.sexp.ISExpression;
import com.larkery.jasb.sexp.ISExpressionVisitor;
import com.larkery.jasb.sexp.Location;

public class Writer {
	private final Set<? extends IAtomWriter> atomWriters;
	
	public Writer(final Set<? extends IAtomWriter> atomWriters) {
		super();
		this.atomWriters = atomWriters;
	}

	public ISExpression write(final Object object, final Function<Object, Optional<Location>> locator) {
		return new WriteSource(object, locator);
	}
	
	class WriteSource implements ISExpression {
		private final Object object;
		private final Function<Object, Optional<Location>> locator;

		public WriteSource(final Object object, final Function<Object, Optional<Location>> locator) {
			this.object = object;
			this.locator = locator;
		}

		@Override
		public void accept(final ISExpressionVisitor visitor) {
			new WriteSession(locator).accept(object, visitor);
		}
	}
	
	class WriteSession {
		private final Map<Object, String> identities = new IdentityHashMap<>();
		private final Function<Object, Optional<Location>> locator;
		
		public WriteSession(final Function<Object, Optional<Location>> locator) {
			this.locator = locator;
		}
		
		public void accept(final Object o, final ISExpressionVisitor visitor) {
			// first try and write o as an atom
			final Optional<Location> location = locator.apply(o);
			if (location.isPresent()) {
				final Location here = location.get();
				visitor.locate(here);
			}
			
			for (final IAtomWriter w : atomWriters) {
				if (w.canWrite(o)) {
					visitor.atom(w.write(o));
					return;
				}
			}
			
			if (identities.containsKey(o)) {
				visitor.atom("#" + identities.get(o));
				return;
			}
			
			if (o.getClass().isAnnotationPresent(PointlessWrapper.class)) {
				// this is a JAXB xml pointless wrapper thing
				// which we want to throw away.
				// it ought to contain a single property of interest
				for (final Method m : o.getClass().getMethods()) {
					if (m.isAnnotationPresent(PointlessWrapper.class)) {
						try {
							final Object o2 = m.invoke(o);
							// transparently do o2 instead of this le
							accept(o2, visitor);
							return;
						} catch (IllegalAccessException
								| IllegalArgumentException
								| InvocationTargetException e) {
							throw new RuntimeException("Pointless wrapper should be on a getter", e);
						}
					}
				}
			}
			
			if (o.getClass().isAnnotationPresent(Bind.class)) {
				final Bind bind = o.getClass().getAnnotation(Bind.class);
				
				visitor.open();
				visitor.atom(bind.value());
				
				final Set<JasbPropertyDescriptor> descriptors = 
						JasbPropertyDescriptor.getDescriptors(o.getClass());
				
				for (final JasbPropertyDescriptor pd : JasbPropertyDescriptor.getPropertiesBoundTo(BoundTo.Name, descriptors)) {
					try {
						final Object value = pd.readMethod.invoke(o);
						
						if (value != null && !(value instanceof List && ((List<?>) value).isEmpty())) {
							if (pd.isIdentifier) {
								identities.put(o, value + "");
							}
							visitor.atom(pd.key.get() + ":");
							accept(value, visitor);
						}
					} catch (IllegalAccessException | IllegalArgumentException
							| InvocationTargetException e) {
						throw new IllegalArgumentException("Failed to invoke getter for " + pd.name, e);
					}
				}
				
				final List<JasbPropertyDescriptor> positional = new ArrayList<>(
						JasbPropertyDescriptor.getPropertiesBoundTo(BoundTo.Position, descriptors)
						);
				
				Collections.sort(positional, new Comparator<JasbPropertyDescriptor>() {
					@Override
					public int compare(final JasbPropertyDescriptor arg0,
							final JasbPropertyDescriptor arg1) {
						return arg0.position.get().compareTo(arg1.position.get());
					}
				});
				
				boolean haveHitNull = false;
				for (final JasbPropertyDescriptor pd : positional) {
					Object value;
					try {
						value = pd.readMethod.invoke(o);
					} catch (IllegalAccessException | IllegalArgumentException
							| InvocationTargetException e) {
						throw new IllegalArgumentException("Failed to invoke getter for " + pd.name, e);
					}
					if (value != null) {
						if (haveHitNull) throw new IllegalArgumentException("Positional argument " + pd.position.get() + " is not null, but a previous positional argument was null, which is an illegal state");
						accept(value, visitor);
					} else {
						haveHitNull = true;
					}
				}
				
				final List<JasbPropertyDescriptor> remainder = new ArrayList<>(
						JasbPropertyDescriptor.getPropertiesBoundTo(BoundTo.Remainder, descriptors)
						);
				
				if (remainder.isEmpty() == false) {
					final JasbPropertyDescriptor remainderProp = remainder.iterator().next();
					Object value;
					try {
						value = remainderProp.readMethod.invoke(o);
					} catch (IllegalAccessException | IllegalArgumentException
							| InvocationTargetException e) {
						throw new IllegalArgumentException("Failed to invoke getter for " + remainderProp.name, e);
					}
					if (value != null) {
						final List<?> things = (List<?>) value;
						if (things.isEmpty() == false) {
							if (haveHitNull) throw new IllegalArgumentException("Remainder argument is not null or empty, but a previous positional argument was null, which is an illegal state");
							for (final Object val : ((List<?>) things)) {
								accept(val, visitor);
							}
						}
					}
				}
				
				visitor.close();
			} else if (o instanceof List) {
				if (((List<?>) o).size() > 0) {
					visitor.open();
					for (final Object val : ((List<?>) o)) {
						accept(val, visitor);
					}
					visitor.close();
				}
			}
		}
	}
}
