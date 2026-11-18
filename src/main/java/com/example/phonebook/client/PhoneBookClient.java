package com.example.phonebook.client;

import com.example.phonebook.CalendarItem;
import com.example.phonebook.ChecklistItem;
import com.example.phonebook.ClockItem;
import com.example.phonebook.NotepadItem;
import com.example.phonebook.PhoneBookItem;
import com.example.phonebook.StopwatchItem;
import com.example.phonebook.TimerItem;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;

public class PhoneBookClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		PhoneBookItem.openScreen = (stack, hand) ->
				MinecraftClient.getInstance().setScreen(new PhoneBookScreen(stack, hand));
		ChecklistItem.openScreen = (stack, hand) ->
				MinecraftClient.getInstance().setScreen(new ChecklistScreen(stack, hand));
		NotepadItem.openScreen = (stack, hand) ->
				MinecraftClient.getInstance().setScreen(new NotepadScreen(stack, hand));
		CalendarItem.openScreen = (stack, hand) ->
				MinecraftClient.getInstance().setScreen(new CalendarScreen(stack, hand));
		ClockItem.openScreen = (stack, hand) ->
				MinecraftClient.getInstance().setScreen(new ClockScreen());
		StopwatchItem.openScreen = (stack, hand) ->
				MinecraftClient.getInstance().setScreen(new StopwatchScreen(hand));
		TimerItem.openScreen = (stack, hand) ->
				MinecraftClient.getInstance().setScreen(new TimerScreen(hand));

		TimerAlerts.register();
	}
}
