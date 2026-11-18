package com.example.phonebook.client;

import com.example.phonebook.StopwatchData;
import com.example.phonebook.StopwatchData.State;
import com.example.phonebook.StopwatchItem;
import com.example.phonebook.TimeFormat;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;

import java.util.ArrayList;
import java.util.List;

/**
 * A stopwatch with lap times. Its state lives in the item and is based on the real-world clock, so it
 * keeps running when this screen is closed.
 */
public class StopwatchScreen extends Screen {
	private static final int PANEL_W = 260;
	private static final int PANEL_H = 226;

	private static final int LIST_ROWS = 6;
	private static final int ROW_H = 14;

	private static final int INK = 0xFF3B2410;
	private static final int MUTED = 0xFF7A6240;
	private static final int GRID = 0xFFB59B66;
	private static final int ROW_A = 0xFFF6EDD2;
	private static final int ROW_B = 0xFFEFE2BC;
	private static final int RUNNING = 0xFF2E7D32;

	private final int slot;

	private int left;
	private int top;
	private ButtonWidget startButton;
	private ButtonWidget lapButton;
	private ButtonWidget resetButton;

	public StopwatchScreen(Hand hand) {
		super(Text.translatable("item.phonebook.stopwatch"));
		this.slot = ClientNet.slotOf(hand);
	}

	// ------------------------------------------------------------------ state

	/** The live stack in the player's inventory (re-fetched, because the server may replace the object). */
	private ItemStack stack() {
		ClientPlayerEntity player = MinecraftClient.getInstance().player;
		return player == null ? ItemStack.EMPTY : player.getInventory().getStack(slot);
	}

	private State state() {
		ItemStack s = stack();
		return s.getItem() instanceof StopwatchItem ? StopwatchData.read(s) : StopwatchData.EMPTY;
	}

	private void apply(State s) {
		ItemStack st = stack();
		if (!(st.getItem() instanceof StopwatchItem)) {
			return;
		}
		StopwatchData.write(st, s); // instant local update; the server copy syncs back
		ClientNet.sendStopwatch(slot, StopwatchData.sanitize(s));
	}

	private void toggle() {
		State s = state();
		long now = System.currentTimeMillis();
		if (s.running()) {
			apply(new State(false, 0, StopwatchData.elapsedNow(s, now), s.laps()));
		} else {
			apply(new State(true, now, s.elapsed(), s.laps()));
		}
	}

	private void lap() {
		State s = state();
		if (!s.running() || s.laps().size() >= StopwatchData.MAX_LAPS) {
			return;
		}
		List<Long> laps = new ArrayList<>(s.laps());
		laps.add(StopwatchData.elapsedNow(s, System.currentTimeMillis()));
		apply(new State(true, s.startedAt(), s.elapsed(), laps));
	}

	private void reset() {
		apply(StopwatchData.EMPTY);
	}

	// ------------------------------------------------------------------ layout

	@Override
	protected void init() {
		left = (width - PANEL_W) / 2;
		top = (height - PANEL_H) / 2;

		int y = top + 68;
		startButton = addDrawableChild(ButtonWidget.builder(Text.literal("Start"), b -> toggle())
				.dimensions(left + 14, y, 88, 20).build());
		lapButton = addDrawableChild(ButtonWidget.builder(Text.literal("Lap"), b -> lap())
				.dimensions(left + 14 + 88 + 6, y, 66, 20).build());
		resetButton = addDrawableChild(ButtonWidget.builder(Text.literal("Reset"), b -> reset())
				.dimensions(left + 14 + 88 + 6 + 66 + 6, y, 66, 20).build());
		tick();
	}

	@Override
	public void tick() {
		if (startButton == null) {
			return;
		}
		State s = state();
		startButton.setMessage(Text.literal(s.running() ? "Stop" : (s.elapsed() > 0 ? "Resume" : "Start")));
		lapButton.active = s.running() && s.laps().size() < StopwatchData.MAX_LAPS;
		resetButton.active = !s.running() && (s.elapsed() > 0 || !s.laps().isEmpty());
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

	/** Skip the vanilla dark gradient/blur so the world stays fully visible behind the stopwatch. */
	@Override
	public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
	}

	// ------------------------------------------------------------------ rendering

	@Override
	public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
		// no renderBackground() call: that is what darkens the world behind the screen

		ctx.fill(left - 3, top - 3, left + PANEL_W + 3, top + PANEL_H + 3, 0xFF3B2410);
		ctx.fill(left, top, left + PANEL_W, top + PANEL_H, 0xFFEFE2BC);

		ctx.drawText(textRenderer, Text.literal("Stopwatch").formatted(Formatting.BOLD), left + 14, top + 10, INK, false);

		State s = state();
		long elapsed = StopwatchData.elapsedNow(s, System.currentTimeMillis());

		// big readout
		String time = TimeFormat.stopwatch(elapsed);
		float scale = 3.0f;
		int w = (int) (textRenderer.getWidth(time) * scale);
		ctx.getMatrices().push();
		ctx.getMatrices().translate(left + (PANEL_W - w) / 2f, top + 30, 0);
		ctx.getMatrices().scale(scale, scale, 1f);
		ctx.drawText(textRenderer, time, 0, 0, s.running() ? RUNNING : INK, false);
		ctx.getMatrices().pop();

		drawLaps(ctx, s);

		super.render(ctx, mouseX, mouseY, delta);
	}

	private void drawLaps(DrawContext ctx, State s) {
		int lx = left + 14;
		int lw = PANEL_W - 28;
		int headerY = top + 96;

		ctx.drawText(textRenderer, "Laps", lx, headerY, INK, false);
		String count = s.laps().size() + (s.laps().size() == 1 ? " lap" : " laps");
		ctx.drawText(textRenderer, count, lx + lw - textRenderer.getWidth(count), headerY, MUTED, false);

		int ly = top + 108;
		ctx.fill(lx - 1, ly - 1, lx + lw + 1, ly + LIST_ROWS * ROW_H + 1, 0xFF6B4A2B);

		List<Long> laps = s.laps();
		for (int r = 0; r < LIST_ROWS; r++) {
			int ry = ly + r * ROW_H;
			ctx.fill(lx, ry, lx + lw, ry + ROW_H - 1, r % 2 == 0 ? ROW_A : ROW_B);

			int index = laps.size() - 1 - r; // newest lap on top
			if (index < 0) {
				continue;
			}
			long total = laps.get(index);
			long split = total - (index > 0 ? laps.get(index - 1) : 0);

			ctx.drawText(textRenderer, "#" + (index + 1), lx + 4, ry + 3, MUTED, false);
			ctx.drawText(textRenderer, TimeFormat.stopwatch(split), lx + 40, ry + 3, INK, false);
			String totalText = TimeFormat.stopwatch(total);
			ctx.drawText(textRenderer, totalText, lx + lw - 4 - textRenderer.getWidth(totalText), ry + 3, MUTED, false);
		}

		if (laps.isEmpty()) {
			String none = "Press Lap while running to record split times.";
			ctx.drawText(textRenderer, none, lx + lw / 2 - textRenderer.getWidth(none) / 2, ly + 3 * ROW_H, MUTED, false);
		}

		int colsY = ly + LIST_ROWS * ROW_H + 6;
		ctx.drawText(textRenderer, "Lap time", lx + 40, colsY, GRID, false);
		ctx.drawText(textRenderer, "Total", lx + lw - 4 - textRenderer.getWidth("Total"), colsY, GRID, false);
	}
}
