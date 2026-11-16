package com.example.phonebook.client;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import com.example.phonebook.PhoneBookItem;

public class PhoneBookClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		PhoneBookItem.openScreen = (stack, hand) ->
				MinecraftClient.getInstance().setScreen(new PhoneBookScreen(stack, hand));
	}
}
