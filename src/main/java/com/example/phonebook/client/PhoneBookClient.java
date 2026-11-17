package com.example.phonebook.client;

import com.example.phonebook.ChecklistItem;
import com.example.phonebook.PhoneBookItem;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;

public class PhoneBookClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		PhoneBookItem.openScreen = (stack, hand) ->
				MinecraftClient.getInstance().setScreen(new PhoneBookScreen(stack, hand));
		ChecklistItem.openScreen = (stack, hand) ->
				MinecraftClient.getInstance().setScreen(new ChecklistScreen(stack, hand));
	}
}
