package com.larkery.jasb.sexp.parse;

import java.io.IOException;
import java.io.Reader;

class StripComments extends Reader {
	private final Reader delegate;
	S state = S.Free;
	Character owed = null;
	public long line = 0;
	public long column = 0;
	public long offset = 0;
	private boolean cr = false;
	
	StripComments(Reader delegate) {
		super();
		this.delegate = delegate;
	}

	@Override
	public void close() throws IOException {
		delegate.close();
	}

	enum S {
		Free,
		Semi,
		Comment
	}
	
	@Override
	public int read(char[] cbuf, int off, int len) throws IOException {
		int val = 0;
		
		int written = 0;
		
		if (owed != null && len > 0) {
			cbuf[off] = owed;
			owed = null;
			written = 1;
		}
		
		while (written < len && (val = delegate.read()) != -1) {
			offset++;
			column++;
			switch (state) {
			case Comment:
				if (val == '\r' || val == '\n') {
					state = S.Free;
					cbuf[off+written] = (char) val;
					written++;
					if (!cr || val == '\r') {
						line++;
						column = 0;
					}
				}
				break;
			case Free:
				if (val == ';') {
					state = S.Semi;
				} else {
					cbuf[off+written] = (char) val;
					written++;
				}
				break;
			case Semi:
				if (val == ';') {
					state = S.Comment;
				} else {
					cbuf[off+written] = ';';
					written++;
					// we now owe val to the stream, but we may not have room for it
					// we can't end up owing more than one thing though so that's OK
					if (written == len) {
						owed = (char) val;
					} else {
						cbuf[off+written] = (char) val;
						written++;
					}
				}
				break;
			}
			
			cr = val == '\r';
		}
		
		if (val == -1 && written == 0) {
			return -1;
		} else {
			return written;
		}
	}
	
	
}
