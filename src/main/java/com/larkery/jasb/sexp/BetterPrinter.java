package com.larkery.jasb.sexp;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;

import com.google.common.base.CharMatcher;
import com.larkery.jasb.sexp.Location.Position;

public class BetterPrinter implements ISExpressionVisitor, AutoCloseable {
	private BufferedWriter output;
	
	private int indentation = 0;
	private Location whereAmI = null;
	private Location toShift = null;
	private boolean inSpace = true;
	private int column = 1;
	private int line = 1;
	
	public BetterPrinter(Writer output) {
		this.output = new BufferedWriter(output);
	}

	public static void print(final ISExpression expression, final Writer output) {
		try (BetterPrinter p = new BetterPrinter(output)) {
			expression.accept(p);
		} catch (IOException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	@Override
	public void locate(Location loc) {
		toShift = loc;
	}

	private void doShift() {
		shift(toShift);
	}
	
	private void shift(Location loc) {	
		if (whereAmI != null && loc != null) {
			if (!(loc.positions.isEmpty() || whereAmI.positions.isEmpty())) {
				final Position prevPosition = whereAmI.getTailPosition();
				final Position curPosition = loc.getTailPosition();
				
				if (prevPosition.name.equals(curPosition.name)) {
					if (line == curPosition.line) {
						shiftColumn(curPosition.column);
					} else if (line < curPosition.line) {
						shiftLine(curPosition.line - line);
					} else {
						shiftLine(1);
					}
				} else {
					shiftLine(1);
				}
				
				line = curPosition.line;
				column = curPosition.column;
			}
		}
		
		whereAmI = loc;
	}

	private void shiftColumn(int column) {
		try {
			while (this.column < column) {
				this.column++;
				output.write(" ");
			}
			inSpace = true;
		} catch (IOException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	private void shiftLine(int j) {
		try {
			while (j > 0) {
				output.write("\n");
				line++;
				for (int i = 0; i<indentation; i++) {
					output.write("\t");
				}
				j--;
			}
			inSpace = true;
			column = 1;
		} catch (IOException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	@Override
	public void open(Delim delimeter) {
		doShift();
		write(""+delimeter.open);
		indentation++;
	}

	private static int lines(final String s) {
		int c = 0;
		boolean wasR = false;
		for (int i =0; i<s.length(); i++) {
			switch (s.charAt(i)) {
			case '\n':
				c++;
				wasR = false;
				break;
			case '\r':
				wasR = true;
				break;
			default:
				if (wasR) {
					c++;
				}
				wasR = false;
			}
		}
		if (wasR) c++;
		return c;
	}
	
	private static final CharMatcher END_TOKEN = CharMatcher.WHITESPACE.and(
			CharMatcher.anyOf(":()[]"));
	
	private void write(String string) {
		try {
			if (!inSpace) {
				shiftLine(1);
				line++;
			}
			
			if (string.contains("\n") || string.contains("\r")) {
				output.write(string);
				column = string.length() - (string.lastIndexOf("\n") + 1);
				line += lines(string);
			} else {
				output.write(string);
				column += string.length();
			}
			
			inSpace = END_TOKEN.matches(string.charAt(string.length()-1));
		} catch (IOException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	@Override
	public void atom(String string) {
		doShift();
		write(Atom.escape(string));
	}

	@Override
	public void comment(String text) {
		doShift();
		write("; " + text);
	}

	@Override
	public void close(Delim delimeter) {
		indentation--;
		doShift();
		write(""+delimeter.close);
	}
	
	@Override
	public void close() throws IOException {
		this.output.close();
	}
}
