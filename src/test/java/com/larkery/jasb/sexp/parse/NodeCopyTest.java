package com.larkery.jasb.sexp.parse;

import com.larkery.jasb.sexp.ISexpSource;
import com.larkery.jasb.sexp.Node;

public class NodeCopyTest extends ParserTest {
	@Override
	protected ISexpSource source(String name, String src) {
		return Node.copy(super.source(name, src));
	}
}
