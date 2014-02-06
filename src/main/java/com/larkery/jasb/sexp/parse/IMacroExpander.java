package com.larkery.jasb.sexp.parse;

import com.larkery.jasb.sexp.ISExpression;

public interface IMacroExpander {

	ISExpression expand(ISExpression input);

}
