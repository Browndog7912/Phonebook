package com.example.phonebook;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.BiConsumer;

public class ClockItem extends Item {
	/** Set by the client initializer so this class never touches client-only code on a dedicated server. */
	public static volatile BiConsumer<ItemStack, Hand> openScreen = (stack, hand) -> {
	};

	public ClockItem(Settings settings) {
		super(settings);
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		ItemStack stack = user.getStackInHand(hand);
		if (world.isClient) {
			openScreen.accept(stack, hand);
		}
		return TypedActionResult.success(stack, world.isClient);
	}

	@Override
	public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
		if (world != null) {
			int tick = GameClock.tickOfDay(world.getTimeOfDay());
			tooltip.add(Text.translatable("tooltip.phonebook.clock_game", GameClock.format(tick, true))
					.formatted(Formatting.GRAY));
		}
		tooltip.add(Text.translatable("tooltip.phonebook.clock_real",
				LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))).formatted(Formatting.GRAY));
	}
}
