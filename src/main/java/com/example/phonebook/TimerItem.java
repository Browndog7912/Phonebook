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

public class TimerItem extends Item {
	/** Set by the client initializer so this class never touches client-only code on a dedicated server. */
	public static volatile BiConsumer<ItemStack, Hand> openScreen = (stack, hand) -> {
	};

	public TimerItem(Settings settings) {
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
		TimerData.State s = TimerData.read(stack);
		if (s.finished()) {
			tooltip.add(Text.translatable("tooltip.phonebook.timer_finished").formatted(Formatting.GOLD));
		} else if (s.running()) {
			String left = TimeFormat.countdown(TimerData.remainingNow(s, System.currentTimeMillis()));
			tooltip.add(Text.translatable("tooltip.phonebook.timer_running", left).formatted(Formatting.GREEN));
		} else if (s.duration() > 0) {
			tooltip.add(Text.translatable("tooltip.phonebook.timer_set", TimeFormat.countdown(s.remaining()))
					.formatted(Formatting.GRAY));
		} else {
			tooltip.add(Text.translatable("tooltip.phonebook.timer_unset").formatted(Formatting.GRAY));
		}
	}
}
