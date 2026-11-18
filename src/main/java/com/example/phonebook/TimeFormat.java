package com.example.phonebook;

import java.util.Locale;

/** Formatting helpers for the stopwatch and timer. */
public final class TimeFormat {
	private TimeFormat() {
	}

	/** Stopwatch style: "MM:SS.cc", or "H:MM:SS.cc" once an hour has passed. */
	public static String stopwatch(long ms) {
		ms = Math.max(0, ms);
		long cs = (ms / 10) % 100;
		long totalSec = ms / 1000;
		long s = totalSec % 60;
		long m = (totalSec / 60) % 60;
		long h = totalSec / 3600;
		if (h > 0) {
			return String.format(Locale.ROOT, "%d:%02d:%02d.%02d", h, m, s, cs);
		}
		return String.format(Locale.ROOT, "%02d:%02d.%02d", m, s, cs);
	}

	/** Countdown style "HH:MM:SS", rounded up to a whole second so it reaches 00:00:00 exactly at the end. */
	public static String countdown(long ms) {
		long sec = (Math.max(0, ms) + 999) / 1000;
		return String.format(Locale.ROOT, "%02d:%02d:%02d", sec / 3600, (sec / 60) % 60, sec % 60);
	}
}
