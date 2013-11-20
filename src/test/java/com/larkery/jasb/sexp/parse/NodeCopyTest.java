package com.larkery.jasb.sexp.parse;

import com.larkery.jasb.sexp.ISExpression;
import com.larkery.jasb.sexp.Node;

public class NodeCopyTest extends ParserTest {
	@Override
	protected ISExpression source(String name, String src) {
		return Node.copy(super.source(name, src));
	}
}
