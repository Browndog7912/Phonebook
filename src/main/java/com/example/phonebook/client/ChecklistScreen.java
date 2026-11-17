package com.example.phonebook.client;

import com.example.phonebook.ChecklistData;
import com.example.phonebook.ChecklistData.Entry;
import com.example.phonebook.PhoneBookNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * The checklist UI: tick boxes to mark tasks done (text is struck through), add new tasks,
 * delete tasks with the X, clear all finished tasks, and flip pages when the list is long.
 */
public class ChecklistScreen extends Screen {
	private static final int PANEL_W = 260;
	private static final int PANEL_H = 232;

	private static final int LIST_X = 14;
	private static final int LIST_Y = 44;
	private static final int LIST_W = PANEL_W - 28;
	private static final int ROW_H = 14;
	private static final int ROWS = 10;

	private static final int BOX = 10;
	private static final int DEL_W = 12;

	private static final int INK = 0xFF3B2410;
	private static final int MUTED = 0xFF7A6240;
	private static final int GRID = 0xFFB59B66;
	private static final int ROW_BG_A = 0xFFF6EDD2;
	private static final int ROW_BG_B = 0xFFEFE2BC;
	private static final int ROW_HOVER = 0xFFDCE9F5;
	private static final int CHECK_GREEN = 0xFF2E7D32;
	private static final int DEL_RED = 0xFFB03A2E;

	private final ItemStack stack;
	private final Hand hand;
	private final List<Entry> entries;

	private int page;
	private int left;
	private int top;

	private TextFieldWidget field;
	private ButtonWidget prevButton;
	private ButtonWidget nextButton;
	private ButtonWidget clearButton;

	public ChecklistScreen(ItemStack stack, Hand hand) {
		super(Text.translatable("item.phonebook.checklist"));
		this.stack = stack;
		this.hand = hand;
		this.entries = new ArrayList<>(ChecklistData.read(stack));
	}

	private int pageCount() {
		return Math.max(1, (entries.size() + ROWS - 1) / ROWS);
	}

	@Override
	protected void init() {
		left = (width - PANEL_W) / 2;
		top = (height - PANEL_H) / 2;

		String old = field == null ? "" : field.getText();

		int fy = top + 44 + ROWS * ROW_H + 8;
		field = new TextFieldWidget(textRenderer, left + LIST_X, fy, LIST_W - 44, 20, Text.literal("New task"));
		field.setMaxLength(ChecklistData.MAX_TEXT);
		field.setSuggestion("New task");
		field.setChangedListener(t -> field.setSuggestion(t.isEmpty() ? "New task" : null));
		field.setText(old);
		addDrawableChild(field);

		addDrawableChild(ButtonWidget.builder(Text.literal("Add"), b -> addEntry())
				.dimensions(left + LIST_X + LIST_W - 40, fy, 40, 20).build());

		prevButton = addDrawableChild(ButtonWidget.builder(Text.literal("<"), b -> changePage(-1))
				.dimensions(left + LIST_X, fy + 24, 30, 20).build());
		clearButton = addDrawableChild(ButtonWidget.builder(Text.literal("Clear done"), b -> clearDone())
				.dimensions(left + PANEL_W / 2 - 40, fy + 24, 80, 20).build());
		nextButton = addDrawableChild(ButtonWidget.builder(Text.literal(">"), b -> changePage(1))
				.dimensions(left + LIST_X + LIST_W - 30, fy + 24, 30, 20).build());

		setInitialFocus(field);
		page = Math.min(page, pageCount() - 1);
		refreshButtons();
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

	/** Skip the vanilla dark gradient/blur so the world stays fully visible behind the checklist. */
	@Override
	public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
	}

	// ------------------------------------------------------------------ editing

	private void refreshButtons() {
		prevButton.active = page > 0;
		nextButton.active = page < pageCount() - 1;
		clearButton.active = ChecklistData.doneCount(entries) > 0;
	}

	private void changePage(int delta) {
		page = Math.max(0, Math.min(pageCount() - 1, page + delta));
		refreshButtons();
	}

	private void addEntry() {
		String text = field.getText().trim();
		if (text.isEmpty() || entries.size() >= ChecklistData.MAX_ENTRIES) {
			return;
		}
		entries.add(new Entry(text, false));
		field.setText("");
		page = pageCount() - 1; // jump to the page holding the new task
		commit();
	}

	private void toggle(int index) {
		Entry e = entries.get(index);
		entries.set(index, new Entry(e.text(), !e.done()));
		commit();
	}

	private void remove(int index) {
		entries.remove(index);
		commit();
	}

	private void clearDone() {
		entries.removeIf(Entry::done);
		commit();
	}

	/** Keeps the page in range, refreshes buttons, updates the local stack and syncs to the server. */
	private void commit() {
		page = Math.max(0, Math.min(pageCount() - 1, page));
		refreshButtons();

		ChecklistData.write(stack, entries); // instant local preview; the server copy syncs back
		PacketByteBuf buf = PacketByteBufs.create();
		buf.writeEnumConstant(hand);
		buf.writeVarInt(entries.size());
		for (Entry e : entries) {
			buf.writeString(e.text());
			buf.writeBoolean(e.done());
		}
		ClientPlayNetworking.send(PhoneBookNetworking.SAVE_CHECKLIST, buf);
	}

	// ------------------------------------------------------------------ input

	@Override
	public boolean mouseClicked(double mx, double my, int button) {
		if (super.mouseClicked(mx, my, button)) {
			return true;
		}
		if (button != 0) {
			return false;
		}
		int lx = left + LIST_X;
		int ly = top + LIST_Y;
		if (mx >= lx && mx < lx + LIST_W && my >= ly && my < ly + ROWS * ROW_H) {
			int r = (int) ((my - ly) / ROW_H);
			int index = page * ROWS + r;
			if (index < entries.size()) {
				if (mx >= lx + LIST_W - DEL_W) {
					remove(index);
				} else {
					toggle(index); // clicking anywhere else on the row toggles it
				}
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean mouseScrolled(double mx, double my, double horizontal, double vertical) {
		if (vertical != 0) {
			changePage(vertical < 0 ? 1 : -1);
			return true;
		}
		return super.mouseScrolled(mx, my, horizontal, vertical);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if ((keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) && field.isFocused()) {
			addEntry();
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	// ------------------------------------------------------------------ rendering

	@Override
	public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
		// no renderBackground() call: that is what darkens the world behind the screen

		ctx.fill(left - 3, top - 3, left + PANEL_W + 3, top + PANEL_H + 3, 0xFF3B2410);
		ctx.fill(left, top, left + PANEL_W, top + PANEL_H, 0xFFEFE2BC);

		ctx.drawText(textRenderer, Text.literal("Checklist").formatted(Formatting.BOLD), left + 14, top + 10, INK, false);

		int done = ChecklistData.doneCount(entries);
		int total = entries.size();
		String progress = done + " / " + total + " done";
		ctx.drawText(textRenderer, progress, left + PANEL_W - 14 - textRenderer.getWidth(progress), top + 10, INK, false);

		// progress bar
		int bx = left + 14;
		int by = top + 26;
		int bw = PANEL_W - 28;
		ctx.fill(bx - 1, by - 1, bx + bw + 1, by + 7, 0xFF6B4A2B);
		ctx.fill(bx, by, bx + bw, by + 6, GRID);
		if (total > 0) {
			ctx.fill(bx, by, bx + (int) ((long) bw * done / total), by + 6, CHECK_GREEN);
		}

		drawList(ctx, mouseX, mouseY);

		String pageLabel = "Page " + (page + 1) + " / " + pageCount();
		ctx.drawText(textRenderer, pageLabel, left + PANEL_W / 2 - textRenderer.getWidth(pageLabel) / 2,
				top + PANEL_H - 14, MUTED, false);

		super.render(ctx, mouseX, mouseY, delta);
	}

	private void drawList(DrawContext ctx, int mouseX, int mouseY) {
		int lx = left + LIST_X;
		int ly = top + LIST_Y;

		ctx.fill(lx - 1, ly - 1, lx + LIST_W + 1, ly + ROWS * ROW_H + 1, 0xFF6B4A2B);

		for (int r = 0; r < ROWS; r++) {
			int ry = ly + r * ROW_H;
			int index = page * ROWS + r;
			boolean has = index < entries.size();
			boolean hover = has && mouseX >= lx && mouseX < lx + LIST_W && mouseY >= ry && mouseY < ry + ROW_H;

			int bg = (r % 2 == 0) ? ROW_BG_A : ROW_BG_B;
			if (hover) {
				bg = ROW_HOVER;
			}
			ctx.fill(lx, ry, lx + LIST_W, ry + ROW_H - 1, bg);
			if (!has) {
				continue;
			}

			Entry e = entries.get(index);

			// checkbox
			int cx = lx + 4;
			int cy = ry + (ROW_H - 1 - BOX) / 2;
			ctx.fill(cx, cy, cx + BOX, cy + BOX, INK);
			ctx.fill(cx + 1, cy + 1, cx + BOX - 1, cy + BOX - 1, 0xFFFFFAEB);
			if (e.done()) {
				drawCheckMark(ctx, cx, cy);
			}

			// text (struck through and muted when done)
			String shown = textRenderer.trimToWidth(e.text(), LIST_W - BOX - DEL_W - 18);
			Text label = Text.literal(shown);
			if (e.done()) {
				label = label.copy().formatted(Formatting.STRIKETHROUGH);
			}
			ctx.drawText(textRenderer, label, cx + BOX + 6, ry + 3, e.done() ? MUTED : INK, false);

			// delete X
			boolean overDel = hover && mouseX >= lx + LIST_W - DEL_W;
			String x = "x";
			ctx.drawText(textRenderer, x, lx + LIST_W - DEL_W + (DEL_W - textRenderer.getWidth(x)) / 2 - 1, ry + 3,
					overDel ? DEL_RED : GRID, false);
		}

		if (entries.isEmpty()) {
			String empty = "Nothing here yet - add a task below.";
			ctx.drawText(textRenderer, empty, lx + LIST_W / 2 - textRenderer.getWidth(empty) / 2,
					ly + 4 * ROW_H, MUTED, false);
		}
	}

	/** A pixel-drawn tick inside a BOX x BOX checkbox (no font glyph needed). */
	private void drawCheckMark(DrawContext ctx, int cx, int cy) {
		int[][] pts = {{2, 5}, {3, 6}, {4, 7}, {5, 6}, {6, 5}, {7, 4}, {8, 3}};
		for (int[] p : pts) {
			ctx.fill(cx + p[0] - 1, cy + p[1] - 1, cx + p[0] + 1, cy + p[1] + 1, CHECK_GREEN);
		}
	}
}
