package com.example.phonebook.client;

import com.example.phonebook.NotepadData;
import com.example.phonebook.PhoneBookNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.EditBoxWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;

/**
 * A free-form multi-line notepad. Type freely; the note is saved to the item automatically
 * whenever the screen closes (Done, Escape, or the inventory key).
 */
public class NotepadScreen extends Screen {
	private static final int PANEL_W = 260;
	private static final int PANEL_H = 232;

	private static final int INK = 0xFF3B2410;
	private static final int MUTED = 0xFF7A6240;

	private final ItemStack stack;
	private final Hand hand;
	private final String original;
	private String current;

	private int left;
	private int top;
	private EditBoxWidget editBox;

	public NotepadScreen(ItemStack stack, Hand hand) {
		super(Text.translatable("item.phonebook.notepad"));
		this.stack = stack;
		this.hand = hand;
		this.original = NotepadData.read(stack);
		this.current = original;
	}

	@Override
	protected void init() {
		left = (width - PANEL_W) / 2;
		top = (height - PANEL_H) / 2;

		editBox = new EditBoxWidget(textRenderer, left + 14, top + 30, PANEL_W - 28, 160,
				Text.literal("Write a note..."), Text.literal("Notepad"));
		editBox.setMaxLength(NotepadData.MAX_CHARS);
		editBox.setText(current); // keeps the text if the window is resized
		editBox.setChangeListener(t -> current = t);
		addDrawableChild(editBox);

		addDrawableChild(ButtonWidget.builder(Text.literal("Done"), b -> close())
				.dimensions(left + PANEL_W - 14 - 60, top + PANEL_H - 30, 60, 20).build());

		setInitialFocus(editBox);
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

	/** Skip the vanilla dark gradient/blur so the world stays fully visible behind the notepad. */
	@Override
	public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
	}

	/** Saves on every way of closing the screen. */
	@Override
	public void removed() {
		super.removed();
		if (current.equals(original)) {
			return;
		}
		NotepadData.write(stack, current); // instant local preview for the tooltip
		PacketByteBuf buf = PacketByteBufs.create();
		buf.writeEnumConstant(hand);
		buf.writeString(current, NotepadData.MAX_CHARS * 2);
		ClientPlayNetworking.send(PhoneBookNetworking.SAVE_NOTEPAD, buf);
	}

	@Override
	public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
		// no renderBackground() call: that is what darkens the world behind the screen

		ctx.fill(left - 3, top - 3, left + PANEL_W + 3, top + PANEL_H + 3, 0xFF3B2410);
		ctx.fill(left, top, left + PANEL_W, top + PANEL_H, 0xFFEFE2BC);

		ctx.drawText(textRenderer, Text.literal("Notepad").formatted(Formatting.BOLD), left + 14, top + 10, INK, false);

		String counter = current.length() + " / " + NotepadData.MAX_CHARS;
		ctx.drawText(textRenderer, counter, left + PANEL_W - 14 - textRenderer.getWidth(counter), top + 10, MUTED, false);

		ctx.drawText(textRenderer, "Saved automatically when you close it", left + 14, top + PANEL_H - 24, MUTED, false);

		super.render(ctx, mouseX, mouseY, delta);
	}
}
