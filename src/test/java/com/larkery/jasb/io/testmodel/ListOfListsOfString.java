package com.larkery.jasb.io.testmodel;

import com.larkery.jasb.bind.Bind;
import com.larkery.jasb.bind.BindRemainingArguments;
import java.util.ArrayList;
import java.util.List;

@Bind("listoflists")
public class ListOfListsOfString {
	private List<List<String>> contents = new ArrayList<>();

	public ListOfListsOfString() {
		
	}

	@BindRemainingArguments
	public List<List<String>> getContents() {
		return contents;
	}

	public void setContents(final List<List<String>> newContents) {
		this.contents = newContents;
	}
}
