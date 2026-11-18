package com.example.phonebook;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;

/**
 * Countdown timer state stored in the item. A running timer stores the real-world epoch millisecond
 * it will finish at, so it keeps counting while the screen is closed.
 */
public final class TimerData {
	public static final long MAX_MS = (99L * 3600 + 59 * 60 + 59) * 1000L; // 99:59:59

	private static final String KEY = "Timer";

	/**
	 * duration: the length that was set. remaining: time left while idle/paused. running: counting down
	 * towards endAt. finished: reached zero (the alarm has gone off).
	 */
	public record State(long duration, long remaining, boolean running, long endAt, boolean finished) {
	}

	public static final State EMPTY = new State(0, 0, false, 0, false);

	private TimerData() {
	}

	private static long clamp(long v, long lo, long hi) {
		return Math.max(lo, Math.min(hi, v));
	}

	public static State sanitize(State s) {
		long d = clamp(s.duration(), 0, MAX_MS);
		long r = clamp(s.remaining(), 0, d);
		boolean running = s.running() && d > 0;
		boolean fin = s.finished() && !running;
		return new State(d, fin ? 0 : r, running, running ? Math.max(0, s.endAt()) : 0, fin);
	}

	public static State read(ItemStack stack) {
		NbtCompound tag = stack.getSubNbt(KEY);
		if (tag == null) {
			return EMPTY;
		}
		return sanitize(new State(tag.getLong("Duration"), tag.getLong("Remaining"), tag.getBoolean("Running"),
				tag.getLong("EndAt"), tag.getBoolean("Finished")));
	}

	public static void write(ItemStack stack, State state) {
		State s = sanitize(state);
		if (s.duration() == 0) {
			stack.removeSubNbt(KEY);
			return;
		}
		NbtCompound tag = stack.getOrCreateSubNbt(KEY);
		tag.putLong("Duration", s.duration());
		tag.putLong("Remaining", s.remaining());
		tag.putBoolean("Running", s.running());
		tag.putLong("EndAt", s.endAt());
		tag.putBoolean("Finished", s.finished());
	}

	// ------------------------------------------------------------------ transitions

	public static long remainingNow(State s, long now) {
		return s.running() ? Math.max(0, s.endAt() - now) : s.remaining();
	}

	/** Idle means a duration is set and nothing has been started yet, so the duration can be edited. */
	public static boolean isSetup(State s) {
		return !s.running() && !s.finished() && s.remaining() == s.duration();
	}

	public static State setDuration(long ms) {
		long d = clamp(ms, 0, MAX_MS);
		return new State(d, d, false, 0, false);
	}

	public static State start(State s, long now) {
		long remaining = s.remaining();
		if (s.finished() || remaining <= 0) {
			remaining = s.duration(); // restart from the full duration
		}
		if (s.duration() <= 0) {
			return s;
		}
		return new State(s.duration(), remaining, true, now + remaining, false);
	}

	public static State pause(State s, long now) {
		return new State(s.duration(), remainingNow(s, now), false, 0, false);
	}

	public static State reset(State s) {
		return new State(s.duration(), s.duration(), false, 0, false);
	}

	public static State finish(State s) {
		return new State(s.duration(), 0, false, 0, true);
	}
}
