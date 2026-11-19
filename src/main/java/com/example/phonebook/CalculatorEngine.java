package com.example.phonebook;

import java.math.BigDecimal;
import java.math.MathContext;

/**
 * The maths behind the calculator, with no Minecraft dependencies. It behaves like a pocket calculator:
 * operations run as soon as the next operator or = is pressed (so 2 + 3 x 4 = 20), "5 + =" gives 10, and
 * pressing = again repeats the last operation. Numbers are exact decimals (0.1 + 0.2 = 0.3), rounded to
 * 12 significant digits.
 */
public final class CalculatorEngine {
	public static final int MAX_DIGITS = 12;

	private static final MathContext MC = new MathContext(12);
	private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

	private String entry = "0";
	private String history = "";
	private BigDecimal acc = BigDecimal.ZERO;
	private char op = 0;
	private boolean awaitingOperand; // an operator was just pressed; entry still shows the left side
	private boolean overwrite;       // the next digit starts a brand new number
	private boolean error;
	private char lastOp = 0;
	private BigDecimal lastOperand = BigDecimal.ZERO;

	/** The number currently shown on the main display line. */
	public String entry() {
		return entry;
	}

	/** The small line above it, e.g. "8 + 3 =" or "12 x". */
	public String history() {
		return history;
	}

	public boolean isError() {
		return error;
	}

	public static String symbol(char op) {
		return switch (op) {
			case '*' -> "×";
			case '/' -> "÷";
			default -> String.valueOf(op);
		};
	}

	// ------------------------------------------------------------------ input

	public void digit(int d) {
		if (d < 0 || d > 9) {
			return;
		}
		if (error) {
			clear();
		}
		startNumberIfNeeded();
		if (digitCount(entry) >= MAX_DIGITS) {
			return;
		}
		if (entry.equals("0")) {
			entry = String.valueOf(d);
		} else if (entry.equals("-0")) {
			entry = "-" + d;
		} else {
			entry += d;
		}
	}

	public void dot() {
		if (error) {
			clear();
		}
		startNumberIfNeeded();
		if (!entry.contains(".") && digitCount(entry) < MAX_DIGITS) {
			entry += ".";
		}
	}

	public void operator(char c) {
		if (error) {
			return;
		}
		if (op != 0 && awaitingOperand) {
			op = c; // pressed twice in a row: just swap the operator
			history = format(acc) + " " + symbol(c);
			return;
		}
		BigDecimal current = parse(entry);
		if (op != 0) {
			BigDecimal r = apply(acc, op, current);
			if (r == null) {
				setError();
				return;
			}
			acc = r;
		} else {
			acc = current;
		}
		op = c;
		lastOp = 0;
		awaitingOperand = true;
		overwrite = true;
		entry = format(acc);
		history = entry + " " + symbol(c);
	}

	public void equals() {
		if (error) {
			return;
		}
		BigDecimal a;
		BigDecimal b;
		char used;
		if (op != 0) {
			a = acc;
			b = awaitingOperand ? acc : parse(entry); // "5 + =" uses 5 on both sides
			used = op;
		} else if (lastOp != 0) {
			a = parse(entry); // repeat the last operation on the current result
			b = lastOperand;
			used = lastOp;
		} else {
			return;
		}
		BigDecimal r = apply(a, used, b);
		if (r == null) {
			setError();
			return;
		}
		history = format(a) + " " + symbol(used) + " " + format(b) + " =";
		lastOp = used;
		lastOperand = b;
		acc = r;
		op = 0;
		entry = format(r);
		awaitingOperand = false;
		overwrite = true;
	}

	public void clear() {
		entry = "0";
		history = "";
		acc = BigDecimal.ZERO;
		op = 0;
		lastOp = 0;
		lastOperand = BigDecimal.ZERO;
		awaitingOperand = false;
		overwrite = false;
		error = false;
	}

	public void backspace() {
		if (error) {
			clear();
			return;
		}
		if (overwrite || awaitingOperand) {
			return; // only the number being typed can be edited
		}
		entry = entry.substring(0, entry.length() - 1);
		if (entry.isEmpty() || entry.equals("-")) {
			entry = "0";
		}
	}

	public void negate() {
		if (error || entry.equals("0")) {
			return;
		}
		entry = entry.startsWith("-") ? entry.substring(1) : "-" + entry;
		if (awaitingOperand) {
			awaitingOperand = false;
			overwrite = true;
		}
	}

	public void percent() {
		if (error) {
			return;
		}
		BigDecimal v = parse(entry);
		BigDecimal r;
		if (op == '+' || op == '-') {
			r = acc.multiply(v).divide(HUNDRED, MC); // 50 + 10 % -> 50 + 5
		} else {
			r = v.divide(HUNDRED, MC);
		}
		finishUnary(r, null);
	}

	public void squareRoot() {
		if (error) {
			return;
		}
		BigDecimal v = parse(entry);
		if (v.signum() < 0) {
			setError();
			return;
		}
		finishUnary(v.sqrt(MC), "√(" + format(v) + ")");
	}

	public void square() {
		if (error) {
			return;
		}
		BigDecimal v = parse(entry);
		finishUnary(v.multiply(v), "sqr(" + format(v) + ")");
	}

	public void reciprocal() {
		if (error) {
			return;
		}
		BigDecimal v = parse(entry);
		if (v.signum() == 0) {
			setError();
			return;
		}
		finishUnary(BigDecimal.ONE.divide(v, MC), "1/(" + format(v) + ")");
	}

	// ------------------------------------------------------------------ output helpers

	/** Text to copy to the clipboard (empty on error). */
	public String copyText() {
		return error ? "" : entry;
	}

	/** For whole numbers of 64 or more, how many Minecraft stacks that is, e.g. "3 stacks + 12". */
	public String stacksHint() {
		if (error) {
			return "";
		}
		BigDecimal v = parse(entry).stripTrailingZeros();
		if (v.scale() > 0 || v.signum() <= 0 || v.compareTo(BigDecimal.valueOf(64)) < 0
				|| v.compareTo(BigDecimal.valueOf(1_000_000_000L)) > 0) {
			return "";
		}
		long n = v.longValueExact();
		long stacks = n / 64;
		long rem = n % 64;
		return stacks + (stacks == 1 ? " stack" : " stacks") + (rem > 0 ? " + " + rem : "");
	}

	// ------------------------------------------------------------------ internals

	private void startNumberIfNeeded() {
		if (overwrite || awaitingOperand) {
			entry = "0";
			overwrite = false;
			awaitingOperand = false;
		}
		if (history.endsWith("=")) {
			history = "";
		}
	}

	private void finishUnary(BigDecimal result, String historyText) {
		if (op == 0 && historyText != null) {
			history = historyText;
		}
		entry = format(result);
		awaitingOperand = false;
		overwrite = true;
	}

	private void setError() {
		error = true;
		entry = "Error";
		history = "";
		op = 0;
		lastOp = 0;
		awaitingOperand = false;
		overwrite = true;
	}

	private static int digitCount(String s) {
		int n = 0;
		for (int i = 0; i < s.length(); i++) {
			if (Character.isDigit(s.charAt(i))) {
				n++;
			}
		}
		return n;
	}

	private static BigDecimal parse(String s) {
		return new BigDecimal(s);
	}

	/** Returns null for a division by zero. */
	private static BigDecimal apply(BigDecimal a, char op, BigDecimal b) {
		return switch (op) {
			case '+' -> a.add(b).round(MC);
			case '-' -> a.subtract(b).round(MC);
			case '*' -> a.multiply(b).round(MC);
			case '/' -> b.signum() == 0 ? null : a.divide(b, MC);
			default -> b;
		};
	}

	public static String format(BigDecimal v) {
		BigDecimal r = v.round(MC);
		if (r.signum() == 0) {
			return "0";
		}
		r = r.stripTrailingZeros();
		String plain = r.toPlainString();
		if (plain.replace("-", "").length() > 14) {
			return r.round(new MathContext(8)).stripTrailingZeros().toString(); // scientific, e.g. 1.2345678E+24
		}
		return plain;
	}
}
