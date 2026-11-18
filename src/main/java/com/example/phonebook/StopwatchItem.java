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

import java.util.List;
import java.util.function.BiConsumer;

public class StopwatchItem extends Item {
	/** Set by the client initializer so this class never touches client-only code on a dedicated server. */
	public static volatile BiConsumer<ItemStack, Hand> openScreen = (stack, hand) -> {
	};

	public StopwatchItem(Settings settings) {
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
		StopwatchData.State s = StopwatchData.read(stack);
		String time = TimeFormat.stopwatch(StopwatchData.elapsedNow(s, System.currentTimeMillis()));
		if (s.running()) {
			tooltip.add(Text.translatable("tooltip.phonebook.stopwatch_running", time).formatted(Formatting.GREEN));
		} else if (s.elapsed() > 0) {
			tooltip.add(Text.translatable("tooltip.phonebook.stopwatch_stopped", time).formatted(Formatting.GRAY));
		} else {
			tooltip.add(Text.translatable("tooltip.phonebook.stopwatch_ready").formatted(Formatting.GRAY));
		}
	}
}
