package com.larkery.jasb;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.reflect.TypeToken;

public class Q {
	@Test
	public void tt() throws NoSuchMethodException, SecurityException {
		final TypeToken<List> t = TypeToken.of(List.class);
		
		final TypeToken<?> asdf = TypeToken.of(Q.class.getDeclaredMethod("things").getGenericReturnType());
		
		Assert.assertTrue(t.isAssignableFrom(asdf));
		
		TypeToken<?> resolveType = asdf.resolveType(List.class.getDeclaredMethod("get", int.class).getGenericReturnType());
	
		//type parameter success, but do we want to carry typetoken around instead?
		// so if we have (if (test) val otherval) bound onto
		// If<T> { @Rest List<T> vals; }
		// we want to check that val and otherval are assignable into list <T> and so on.
		Assert.assertEquals(Double.class, resolveType.getRawType());
	}
	
	public List<Double> things() {
		return null;
	}
}
