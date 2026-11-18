package com.example.phonebook.client;

import com.example.phonebook.PhoneBookNetworking;
import com.example.phonebook.StopwatchData;
import com.example.phonebook.TimerData;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Hand;

/** Client-side senders and helpers shared by the stopwatch and timer. */
public final class ClientNet {
	private ClientNet() {
	}

	/** The inventory slot index (0-40) that holds the item the player used from the given hand. */
	public static int slotOf(Hand hand) {
		ClientPlayerEntity player = MinecraftClient.getInstance().player;
		if (player == null || hand == Hand.OFF_HAND) {
			return PlayerInventory.OFF_HAND_SLOT;
		}
		return player.getInventory().selectedSlot;
	}

	public static void sendStopwatch(int slot, StopwatchData.State s) {
		PacketByteBuf buf = PacketByteBufs.create();
		buf.writeVarInt(slot);
		buf.writeBoolean(s.running());
		buf.writeLong(s.startedAt());
		buf.writeLong(s.elapsed());
		buf.writeVarInt(s.laps().size());
		for (long lap : s.laps()) {
			buf.writeLong(lap);
		}
		ClientPlayNetworking.send(PhoneBookNetworking.SAVE_STOPWATCH, buf);
	}

	public static void sendTimer(int slot, TimerData.State s) {
		PacketByteBuf buf = PacketByteBufs.create();
		buf.writeVarInt(slot);
		buf.writeLong(s.duration());
		buf.writeLong(s.remaining());
		buf.writeBoolean(s.running());
		buf.writeLong(s.endAt());
		buf.writeBoolean(s.finished());
		ClientPlayNetworking.send(PhoneBookNetworking.SAVE_TIMER, buf);
	}
}
