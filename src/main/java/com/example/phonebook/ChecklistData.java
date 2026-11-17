package com.example.phonebook;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;

import java.util.ArrayList;
import java.util.List;

/** Checklist storage (item NBT), shared by client and server. */
public final class ChecklistData {
	public static final int MAX_ENTRIES = 200;
	public static final int MAX_TEXT = 48;

	private static final String KEY = "Entries";

	public record Entry(String text, boolean done) {
	}

	private ChecklistData() {
	}

	public static List<Entry> sanitize(List<Entry> in) {
		List<Entry> out = new ArrayList<>();
		for (Entry e : in) {
			String text = e.text().trim();
			if (text.isEmpty()) {
				continue;
			}
			if (text.length() > MAX_TEXT) {
				text = text.substring(0, MAX_TEXT);
			}
			out.add(new Entry(text, e.done()));
			if (out.size() >= MAX_ENTRIES) {
				break;
			}
		}
		return out;
	}

	public static List<Entry> read(ItemStack stack) {
		List<Entry> out = new ArrayList<>();
		NbtCompound tag = stack.getNbt();
		if (tag == null || !tag.contains(KEY, NbtElement.LIST_TYPE)) {
			return out;
		}
		NbtList list = tag.getList(KEY, NbtElement.COMPOUND_TYPE);
		for (int i = 0; i < list.size(); i++) {
			NbtCompound e = list.getCompound(i);
			out.add(new Entry(e.getString("Text"), e.getBoolean("Done")));
		}
		return sanitize(out);
	}

	public static void write(ItemStack stack, List<Entry> entries) {
		List<Entry> clean = sanitize(entries);
		if (clean.isEmpty()) {
			stack.removeSubNbt(KEY);
			return;
		}
		NbtList list = new NbtList();
		for (Entry e : clean) {
			NbtCompound c = new NbtCompound();
			c.putString("Text", e.text());
			c.putBoolean("Done", e.done());
			list.add(c);
		}
		stack.getOrCreateNbt().put(KEY, list);
	}

	public static int doneCount(List<Entry> entries) {
		int n = 0;
		for (Entry e : entries) {
			if (e.done()) {
				n++;
			}
		}
		return n;
	}
}
