package com.example.phonebook;

import java.util.Locale;

/**
 * Converts Minecraft's time of day into a 24-hour clock. Tick 0 is 6:00 AM, tick 6000 is noon,
 * tick 12000 is 6:00 PM and tick 18000 is midnight.
 */
public final class GameClock {
	public static final int DAY_TICKS = 24000;

	private GameClock() {
	}

	public static int tickOfDay(long timeOfDay) {
		return (int) (((timeOfDay % DAY_TICKS) + DAY_TICKS) % DAY_TICKS);
	}

	public static long dayNumber(long timeOfDay) {
		return Math.max(0, timeOfDay) / DAY_TICKS + 1;
	}

	public static int hour(int tick) {
		return (6 + tick / 1000) % 24;
	}

	public static int minute(int tick) {
		return (tick % 1000) * 60 / 1000;
	}

	/** Clock hands position in hours (0-24, fractional) so the analog hands move smoothly. */
	public static double hoursFloat(int tick) {
		return (6.0 + tick / 1000.0) % 24.0;
	}

	public static String format(int tick, boolean use24h) {
		int h = hour(tick);
		int m = minute(tick);
		if (use24h) {
			return String.format(Locale.ROOT, "%02d:%02d", h, m);
		}
		int h12 = h % 12 == 0 ? 12 : h % 12;
		return String.format(Locale.ROOT, "%d:%02d %s", h12, m, h < 12 ? "AM" : "PM");
	}

	public static String phase(int tick) {
		if (tick < 12000) {
			return "Daytime";
		}
		if (tick < 13000) {
			return "Dusk";
		}
		if (tick < 23000) {
			return "Night";
		}
		return "Dawn";
	}

	/** ARGB colour used for the little sun/moon swatch next to the phase name. */
	public static int phaseColor(int tick) {
		if (tick < 12000) {
			return 0xFFE8B923;
		}
		if (tick < 13000 || tick >= 23000) {
			return 0xFFE07B39;
		}
		return 0xFF3B4A8C;
	}
}
