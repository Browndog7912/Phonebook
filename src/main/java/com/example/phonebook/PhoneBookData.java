package com.example.phonebook;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Contact storage (item NBT) plus the alphabet-section sorting rules shared by client and server. */
public final class PhoneBookData {
	public static final int MAX_CONTACTS = 300;
	public static final int MAX_NAME = 32;
	public static final int MAX_NUMBER = 24;

	private static final String KEY = "Contacts";

	public record Contact(String name, String number) {
	}

	/** Sections A-Z first, everything else ("#") last; then by name, then by number. */
	public static final Comparator<Contact> ORDER = Comparator
			.comparingInt((Contact c) -> rank(sectionOf(c.name())))
			.thenComparing(c -> sortKey(c.name()))
			.thenComparing(Contact::number);

	private PhoneBookData() {
	}

	private static int rank(char section) {
		return section == '#' ? 'Z' + 1 : section;
	}

	private static String stripAccents(String s) {
		StringBuilder out = new StringBuilder();
		for (char c : Normalizer.normalize(s, Normalizer.Form.NFD).toCharArray()) {
			if (Character.getType(c) != Character.NON_SPACING_MARK) {
				out.append(c);
			}
		}
		return out.toString();
	}

	private static String sortKey(String name) {
		return stripAccents(name.trim()).toLowerCase(Locale.ROOT);
	}

	/** The alphabet section a name belongs to: 'A'-'Z', or '#' for digits, symbols and other scripts. */
	public static char sectionOf(String name) {
		String s = stripAccents(name.trim());
		if (s.isEmpty()) {
			return '#';
		}
		char c = Character.toUpperCase(s.charAt(0));
		return (c >= 'A' && c <= 'Z') ? c : '#';
	}

	public static List<Contact> sanitize(List<Contact> in) {
		List<Contact> out = new ArrayList<>();
		for (Contact c : in) {
			String name = clamp(c.name().trim(), MAX_NAME);
			if (name.isEmpty()) {
				continue;
			}
			out.add(new Contact(name, clamp(c.number().trim(), MAX_NUMBER)));
			if (out.size() >= MAX_CONTACTS) {
				break;
			}
		}
		out.sort(ORDER);
		return out;
	}

	private static String clamp(String s, int max) {
		return s.length() > max ? s.substring(0, max) : s;
	}

	public static List<Contact> read(ItemStack stack) {
		List<Contact> out = new ArrayList<>();
		NbtCompound tag = stack.getNbt();
		if (tag == null || !tag.contains(KEY, NbtElement.LIST_TYPE)) {
			return out;
		}
		NbtList list = tag.getList(KEY, NbtElement.COMPOUND_TYPE);
		for (int i = 0; i < list.size(); i++) {
			NbtCompound entry = list.getCompound(i);
			out.add(new Contact(entry.getString("Name"), entry.getString("Number")));
		}
		return sanitize(out);
	}

	public static void write(ItemStack stack, List<Contact> contacts) {
		List<Contact> clean = sanitize(contacts);
		if (clean.isEmpty()) {
			stack.removeSubNbt(KEY);
			return;
		}
		NbtList list = new NbtList();
		for (Contact c : clean) {
			NbtCompound entry = new NbtCompound();
			entry.putString("Name", c.name());
			entry.putString("Number", c.number());
			list.add(entry);
		}
		stack.getOrCreateNbt().put(KEY, list);
	}
}
