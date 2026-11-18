package com.example.phonebook.client;

import com.example.phonebook.TimerData;
import com.example.phonebook.TimerItem;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Watches the player's inventory for running timers that have run out, so the alarm goes off even when
 * the timer screen is closed. A finished timer is marked as finished (locally and on the server).
 */
public final class TimerAlerts {
	private static final int ALARM_TICKS = 48; // six chimes, one every 8 ticks

	private static int tickCounter;
	private static int alarmTicks;

	private TimerAlerts() {
	}

	public static void register() {
		ClientTickEvents.END_CLIENT_TICK.register(TimerAlerts::onTick);
	}

	private static void onTick(MinecraftClient client) {
		ClientPlayerEntity player = client.player;
		if (player == null) {
			alarmTicks = 0;
			return;
		}

		if (alarmTicks > 0) {
			if (alarmTicks % 8 == 0) {
				float pitch = (alarmTicks / 8) % 2 == 0 ? 1.6f : 1.2f;
				player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, pitch);
			}
			alarmTicks--;
		}

		if (++tickCounter % 10 != 0) {
			return;
		}

		long now = System.currentTimeMillis();
		PlayerInventory inv = player.getInventory();
		for (int slot = 0; slot < inv.size(); slot++) {
			ItemStack stack = inv.getStack(slot);
			if (!(stack.getItem() instanceof TimerItem)) {
				continue;
			}
			TimerData.State s = TimerData.read(stack);
			if (s.running() && now >= s.endAt()) {
				TimerData.State done = TimerData.finish(s);
				TimerData.write(stack, done); // instant local update
				ClientNet.sendTimer(slot, done);

				alarmTicks = ALARM_TICKS;
				player.sendMessage(Text.literal("Timer finished!").formatted(Formatting.GOLD, Formatting.BOLD), false);
				player.sendMessage(Text.literal("Timer finished!").formatted(Formatting.GOLD, Formatting.BOLD), true);
			}
		}
	}
}
