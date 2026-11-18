package com.example.phonebook;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;

import java.util.ArrayList;
import java.util.List;

/**
 * Stopwatch state stored in the item. It is based on the real-world clock (epoch milliseconds), so a
 * running stopwatch keeps counting even while the screen is closed.
 */
public final class StopwatchData {
	public static final long MAX_MS = 359_999_990L; // 99:59:59.99
	public static final int MAX_LAPS = 99;

	private static final String KEY = "Stopwatch";

	/** laps holds cumulative elapsed time at each lap press. */
	public record State(boolean running, long startedAt, long elapsed, List<Long> laps) {
	}

	public static final State EMPTY = new State(false, 0, 0, List.of());

	private StopwatchData() {
	}

	public static long elapsedNow(State s, long now) {
		long e = s.elapsed() + (s.running() ? Math.max(0, now - s.startedAt()) : 0);
		return Math.min(MAX_MS, Math.max(0, e));
	}

	public static State sanitize(State s) {
		List<Long> laps = new ArrayList<>();
		for (long l : s.laps()) {
			laps.add(Math.min(MAX_MS, Math.max(0, l)));
			if (laps.size() >= MAX_LAPS) {
				break;
			}
		}
		return new State(s.running(), Math.max(0, s.startedAt()), Math.min(MAX_MS, Math.max(0, s.elapsed())), laps);
	}

	public static State read(ItemStack stack) {
		NbtCompound tag = stack.getSubNbt(KEY);
		if (tag == null) {
			return EMPTY;
		}
		List<Long> laps = new ArrayList<>();
		for (long l : tag.getLongArray("Laps")) {
			laps.add(l);
		}
		return sanitize(new State(tag.getBoolean("Running"), tag.getLong("StartedAt"), tag.getLong("Elapsed"), laps));
	}

	public static void write(ItemStack stack, State state) {
		State s = sanitize(state);
		if (!s.running() && s.elapsed() == 0 && s.laps().isEmpty()) {
			stack.removeSubNbt(KEY);
			return;
		}
		NbtCompound tag = stack.getOrCreateSubNbt(KEY);
		tag.putBoolean("Running", s.running());
		tag.putLong("StartedAt", s.startedAt());
		tag.putLong("Elapsed", s.elapsed());
		tag.putLongArray("Laps", s.laps().stream().mapToLong(Long::longValue).toArray());
	}
}
