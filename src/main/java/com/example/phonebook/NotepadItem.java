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

public class NotepadItem extends Item {
	/** Set by the client initializer so this class never touches client-only code on a dedicated server. */
	public static volatile BiConsumer<ItemStack, Hand> openScreen = (stack, hand) -> {
	};

	public NotepadItem(Settings settings) {
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
		String note = NotepadData.read(stack).strip();
		if (note.isEmpty()) {
			tooltip.add(Text.translatable("tooltip.phonebook.notepad_empty").formatted(Formatting.GRAY));
			return;
		}
		String firstLine = note.split("\n", 2)[0];
		if (firstLine.length() > 28) {
			firstLine = firstLine.substring(0, 28) + "...";
		}
		tooltip.add(Text.literal(firstLine).formatted(Formatting.GRAY));
	}
}
