package com.example.phonebook;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
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

	public static final Item PHONE_BOOK = Registry.register(
			Registries.ITEM,
			new Identifier(MOD_ID, "phone_book"),
			new PhoneBookItem(new FabricItemSettings().maxCount(1))
	);

	@Override
	public void onInitialize() {
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> entries.add(PHONE_BOOK));

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
	}
}
