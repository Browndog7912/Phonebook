package com.example.phonebook;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public class PhoneBookMod implements ModInitializer {
	public static final String MOD_ID = "phonebook";

	public static final Item PHONE_BOOK = register("phone_book", new PhoneBookItem(new FabricItemSettings().maxCount(1)));
	public static final Item CHECKLIST = register("checklist", new ChecklistItem(new FabricItemSettings().maxCount(1)));
	public static final Item NOTEPAD = register("notepad", new NotepadItem(new FabricItemSettings().maxCount(1)));
	public static final Item CALENDAR = register("calendar", new CalendarItem(new FabricItemSettings().maxCount(1)));
	public static final Item CLOCK = register("clock", new ClockItem(new FabricItemSettings().maxCount(1)));
	public static final Item STOPWATCH = register("stopwatch", new StopwatchItem(new FabricItemSettings().maxCount(1)));
	public static final Item TIMER = register("timer", new TimerItem(new FabricItemSettings().maxCount(1)));

	private static Item register(String id, Item item) {
		return Registry.register(Registries.ITEM, new Identifier(MOD_ID, id), item);
	}

	/** Returns the stack in an inventory slot index (0-40), or empty if the index is out of range. */
	private static ItemStack stackAt(PlayerEntity player, int slot) {
		if (slot < 0 || slot >= player.getInventory().size()) {
			return ItemStack.EMPTY;
		}
		return player.getInventory().getStack(slot);
	}

	@Override
	public void onInitialize() {
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> {
			entries.add(PHONE_BOOK);
			entries.add(CHECKLIST);
			entries.add(NOTEPAD);
			entries.add(CALENDAR);
			entries.add(CLOCK);
			entries.add(STOPWATCH);
			entries.add(TIMER);
		});

		// Client sends the full contact list; the server validates, sorts and stores it on the held book.
		ServerPlayNetworking.registerGlobalReceiver(PhoneBookNetworking.SAVE, (server, player, handler, buf, responseSender) -> {
			Hand hand = buf.readEnumConstant(Hand.class);
			int count = buf.readVarInt();
			if (count < 0 || count > PhoneBookData.MAX_CONTACTS) {
				return;
			}
			List<PhoneBookData.Contact> list = new ArrayList<>(count);
			for (int i = 0; i < count; i++) {
				String name = buf.readString(64);
				String number = buf.readString(48);
				list.add(new PhoneBookData.Contact(name, number));
			}
			server.execute(() -> {
				ItemStack stack = player.getStackInHand(hand);
				if (stack.getItem() instanceof PhoneBookItem) {
					PhoneBookData.write(stack, list);
				}
			});
		});

		// Client sends the full checklist; the server validates and stores it on the held checklist.
		ServerPlayNetworking.registerGlobalReceiver(PhoneBookNetworking.SAVE_CHECKLIST, (server, player, handler, buf, responseSender) -> {
			Hand hand = buf.readEnumConstant(Hand.class);
			int count = buf.readVarInt();
			if (count < 0 || count > ChecklistData.MAX_ENTRIES) {
				return;
			}
			List<ChecklistData.Entry> list = new ArrayList<>(count);
			for (int i = 0; i < count; i++) {
				String text = buf.readString(96);
				boolean done = buf.readBoolean();
				list.add(new ChecklistData.Entry(text, done));
			}
			server.execute(() -> {
				ItemStack stack = player.getStackInHand(hand);
				if (stack.getItem() instanceof ChecklistItem) {
					ChecklistData.write(stack, list);
				}
			});
		});

		// Client sends the whole note text; the server clamps it and stores it on the held notepad.
		ServerPlayNetworking.registerGlobalReceiver(PhoneBookNetworking.SAVE_NOTEPAD, (server, player, handler, buf, responseSender) -> {
			Hand hand = buf.readEnumConstant(Hand.class);
			String text = buf.readString(NotepadData.MAX_CHARS * 2);
			server.execute(() -> {
				ItemStack stack = player.getStackInHand(hand);
				if (stack.getItem() instanceof NotepadItem) {
					NotepadData.write(stack, text);
				}
			});
		});

		// Client sends the full event list; the server validates, sorts by date and stores it on the held calendar.
		ServerPlayNetworking.registerGlobalReceiver(PhoneBookNetworking.SAVE_CALENDAR, (server, player, handler, buf, responseSender) -> {
			Hand hand = buf.readEnumConstant(Hand.class);
			int count = buf.readVarInt();
			if (count < 0 || count > CalendarData.MAX_EVENTS) {
				return;
			}
			List<CalendarData.Event> list = new ArrayList<>(count);
			for (int i = 0; i < count; i++) {
				int date = buf.readInt();
				String text = buf.readString(96);
				list.add(new CalendarData.Event(date, text));
			}
			server.execute(() -> {
				ItemStack stack = player.getStackInHand(hand);
				if (stack.getItem() instanceof CalendarItem) {
					CalendarData.write(stack, list);
				}
			});
		});

		// Stopwatch: the client sends its full state and the inventory slot of the stopwatch.
		ServerPlayNetworking.registerGlobalReceiver(PhoneBookNetworking.SAVE_STOPWATCH, (server, player, handler, buf, responseSender) -> {
			int slot = buf.readVarInt();
			boolean running = buf.readBoolean();
			long startedAt = buf.readLong();
			long elapsed = buf.readLong();
			int count = buf.readVarInt();
			if (count < 0 || count > StopwatchData.MAX_LAPS) {
				return;
			}
			List<Long> laps = new ArrayList<>(count);
			for (int i = 0; i < count; i++) {
				laps.add(buf.readLong());
			}
			server.execute(() -> {
				ItemStack stack = stackAt(player, slot);
				if (stack.getItem() instanceof StopwatchItem) {
					StopwatchData.write(stack, new StopwatchData.State(running, startedAt, elapsed, laps));
				}
			});
		});

		// Timer: the client sends its full state and the inventory slot of the timer.
		ServerPlayNetworking.registerGlobalReceiver(PhoneBookNetworking.SAVE_TIMER, (server, player, handler, buf, responseSender) -> {
			int slot = buf.readVarInt();
			long duration = buf.readLong();
			long remaining = buf.readLong();
			boolean running = buf.readBoolean();
			long endAt = buf.readLong();
			boolean finished = buf.readBoolean();
			server.execute(() -> {
				ItemStack stack = stackAt(player, slot);
				if (stack.getItem() instanceof TimerItem) {
					TimerData.write(stack, new TimerData.State(duration, remaining, running, endAt, finished));
				}
			});
		});
	}
}
