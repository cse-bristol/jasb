package com.larkery.jasb.sexp.errors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.larkery.jasb.sexp.Location;
import com.larkery.jasb.sexp.Node;

public class UnexpectedTermError extends BasicError {
	private final Set<String> legalValues;
	private final String value;
	
	public UnexpectedTermError(
			final Node node,
            final String type,
			final Set<String> legalValues,
			final String value
			) {
		
		super(node.getLocation() == null ? Collections.<Location>emptySet() : ImmutableSet.of(node.getLocation()), 
			  ImmutableSet.of(node), 
			  createMessage(type, value, legalValues), 
			  Type.ERROR);

		this.legalValues = legalValues;
		this.value = value;
	}

	private static String createMessage(final String type, final String value, final Set<String> legalValues_) {
		final String expected;
		
		final List<String> legalValues = new ArrayList<String>(legalValues_);
		Collections.sort(legalValues, Distance.to(value));
		
		if (legalValues.isEmpty()) {
			expected = "nothing here";
		} else if (legalValues.size() == 1) {
			expected = Iterables.get(legalValues, 0);
		} else if (legalValues.size() == 2) {
			expected = String.format("%s or %s", Iterables.get(legalValues, 0), Iterables.get(legalValues, 1));
		} else {
			final StringBuffer sb = new StringBuffer();
			
			sb.append("one of ");
			
			final Iterator<String> iterator = legalValues.iterator();
			
			String previous = iterator.next();
			do {
				sb.append(previous);
				
				previous = iterator.next();
				if (iterator.hasNext()) {
					sb.append(", ");
				} else {
					sb.append(" or ");
				}
			} while (iterator.hasNext());
			
			sb.append(previous);
			
			expected = sb.toString();
		}
		return String.format("unexpected %s %s; expected %s", type, value, expected);
	}

	public Set<String> getLegalValues() {
		return legalValues;
	}
	
	public String getValue() {
		return value;
	}
}
