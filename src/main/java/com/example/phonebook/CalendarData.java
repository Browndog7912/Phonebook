package com.example.phonebook;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Calendar storage (item NBT), shared by client and server. Dates are encoded as yyyyMMdd ints. */
public final class CalendarData {
	public static final int MAX_EVENTS = 300;
	public static final int MAX_TEXT = 48;

	private static final String KEY = "Events";

	public record Event(int date, String text) {
	}

	private CalendarData() {
	}

	public static int key(LocalDate d) {
		return d.getYear() * 10000 + d.getMonthValue() * 100 + d.getDayOfMonth();
	}

	public static boolean validDate(int key) {
		try {
			LocalDate.of(key / 10000, (key / 100) % 100, key % 100);
			return true;
		} catch (DateTimeException e) {
			return false;
		}
	}

	/** Drops empty/invalid entries, clamps text, caps the count and sorts by date (stable). */
	public static List<Event> sanitize(List<Event> in) {
		List<Event> out = new ArrayList<>();
		for (Event e : in) {
			String text = e.text().trim();
			if (text.isEmpty() || !validDate(e.date())) {
				continue;
			}
			if (text.length() > MAX_TEXT) {
				text = text.substring(0, MAX_TEXT);
			}
			out.add(new Event(e.date(), text));
			if (out.size() >= MAX_EVENTS) {
				break;
			}
		}
		out.sort(Comparator.comparingInt(Event::date));
		return out;
	}

	public static List<Event> read(ItemStack stack) {
		List<Event> out = new ArrayList<>();
		NbtCompound tag = stack.getNbt();
		if (tag == null || !tag.contains(KEY, NbtElement.LIST_TYPE)) {
			return out;
		}
		NbtList list = tag.getList(KEY, NbtElement.COMPOUND_TYPE);
		for (int i = 0; i < list.size(); i++) {
			NbtCompound c = list.getCompound(i);
			out.add(new Event(c.getInt("Date"), c.getString("Text")));
		}
		return sanitize(out);
	}

	public static void write(ItemStack stack, List<Event> events) {
		List<Event> clean = sanitize(events);
		if (clean.isEmpty()) {
			stack.removeSubNbt(KEY);
			return;
		}
		NbtList list = new NbtList();
		for (Event e : clean) {
			NbtCompound c = new NbtCompound();
			c.putInt("Date", e.date());
			c.putString("Text", e.text());
			list.add(c);
		}
		stack.getOrCreateNbt().put(KEY, list);
	}
}
