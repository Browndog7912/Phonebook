package com.example.phonebook;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;

/** Notepad storage (item NBT), shared by client and server. */
public final class NotepadData {
	public static final int MAX_CHARS = 2000;

	private static final String KEY = "Note";

	private NotepadData() {
	}

	public static String sanitize(String text) {
		String s = text.replace("\r", "");
		return s.length() > MAX_CHARS ? s.substring(0, MAX_CHARS) : s;
	}

	public static String read(ItemStack stack) {
		NbtCompound tag = stack.getNbt();
		if (tag == null || !tag.contains(KEY, NbtElement.STRING_TYPE)) {
			return "";
		}
		return sanitize(tag.getString(KEY));
	}

	public static void write(ItemStack stack, String text) {
		String clean = sanitize(text);
		if (clean.isBlank()) {
			stack.removeSubNbt(KEY);
			return;
		}
		stack.getOrCreateNbt().putString(KEY, clean);
	}
}
