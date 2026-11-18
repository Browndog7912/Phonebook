package com.example.phonebook.client;

import com.example.phonebook.TimeFormat;
import com.example.phonebook.TimerData;
import com.example.phonebook.TimerData.State;
import com.example.phonebook.TimerItem;
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
 * A countdown timer. Set the time with the +/- buttons or a preset, then Start. The state lives in the
 * item and is based on the real-world clock, so the timer keeps running (and still sounds the alarm)
 * with this screen closed, as long as the item is in your inventory.
 */
public class TimerScreen extends Screen {
	private static final int PANEL_W = 260;
	private static final int PANEL_H = 232;

	private static final long HOUR = 3_600_000L;
	private static final long MINUTE = 60_000L;
	private static final long SECOND = 1_000L;

	private static final int INK = 0xFF3B2410;
	private static final int MUTED = 0xFF7A6240;
	private static final int GRID = 0xFFB59B66;
	private static final int RUNNING = 0xFF2E7D32;
	private static final int ALARM = 0xFFB03A2E;
	private static final int PAUSED = 0xFF2F5FA8;

	private final int slot;

	private int left;
	private int top;
	private ButtonWidget startButton;
	private ButtonWidget resetButton;
	private final List<ButtonWidget> setupButtons = new ArrayList<>();

	public TimerScreen(Hand hand) {
		super(Text.translatable("item.phonebook.timer"));
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
		return s.getItem() instanceof TimerItem ? TimerData.read(s) : TimerData.EMPTY;
	}

	private void apply(State s) {
		ItemStack st = stack();
		if (!(st.getItem() instanceof TimerItem)) {
			return;
		}
		TimerData.write(st, s); // instant local update; the server copy syncs back
		ClientNet.sendTimer(slot, TimerData.sanitize(s));
	}

	private void adjust(long delta) {
		State s = state();
		if (TimerData.isSetup(s) || s.duration() == 0) {
			apply(TimerData.setDuration(s.duration() + delta));
		}
	}

	private void preset(long ms) {
		if (TimerData.isSetup(state())) {
			apply(TimerData.setDuration(ms));
		}
	}

	private void startPause() {
		State s = state();
		long now = System.currentTimeMillis();
		apply(s.running() ? TimerData.pause(s, now) : TimerData.start(s, now));
	}

	private void reset() {
		apply(TimerData.reset(state()));
	}

	// ------------------------------------------------------------------ layout

	@Override
	protected void init() {
		left = (width - PANEL_W) / 2;
		top = (height - PANEL_H) / 2;
		setupButtons.clear();

		long[] units = {HOUR, MINUTE, SECOND};
		for (int i = 0; i < 3; i++) {
			int cx = left + 14 + 39 + i * 77;
			final long unit = units[i];
			setupButtons.add(addDrawableChild(ButtonWidget.builder(Text.literal("+"), b -> adjust(unit))
					.dimensions(cx - 15, top + 102, 30, 20).build()));
			setupButtons.add(addDrawableChild(ButtonWidget.builder(Text.literal("-"), b -> adjust(-unit))
					.dimensions(cx - 15, top + 142, 30, 20).build()));
		}

		String[] labels = {"1m", "5m", "10m", "30m"};
		long[] values = {MINUTE, 5 * MINUTE, 10 * MINUTE, 30 * MINUTE};
		for (int i = 0; i < 4; i++) {
			final long value = values[i];
			setupButtons.add(addDrawableChild(ButtonWidget.builder(Text.literal(labels[i]), b -> preset(value))
					.dimensions(left + 14 + i * 60, top + 170, 52, 20).build()));
		}

		startButton = addDrawableChild(ButtonWidget.builder(Text.literal("Start"), b -> startPause())
				.dimensions(left + 14, top + 198, 112, 20).build());
		resetButton = addDrawableChild(ButtonWidget.builder(Text.literal("Reset"), b -> reset())
				.dimensions(left + 14 + 112 + 8, top + 198, 112, 20).build());
		tick();
	}

	@Override
	public void tick() {
		if (startButton == null) {
			return;
		}
		State s = state();
		boolean setup = TimerData.isSetup(s) || s.duration() == 0;
		for (ButtonWidget b : setupButtons) {
			b.active = setup;
		}
		String label;
		if (s.running()) {
			label = "Pause";
		} else if (s.finished()) {
			label = "Restart";
		} else if (!TimerData.isSetup(s)) {
			label = "Resume";
		} else {
			label = "Start";
		}
		startButton.setMessage(Text.literal(label));
		startButton.active = s.running() || s.duration() > 0;
		resetButton.active = s.duration() > 0 && !TimerData.isSetup(s);
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

	/** Skip the vanilla dark gradient/blur so the world stays fully visible behind the timer. */
	@Override
	public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
	}

	// ------------------------------------------------------------------ rendering

	@Override
	public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
		// no renderBackground() call: that is what darkens the world behind the screen

		ctx.fill(left - 3, top - 3, left + PANEL_W + 3, top + PANEL_H + 3, 0xFF3B2410);
		ctx.fill(left, top, left + PANEL_W, top + PANEL_H, 0xFFEFE2BC);

		ctx.drawText(textRenderer, Text.literal("Timer").formatted(Formatting.BOLD), left + 14, top + 10, INK, false);

		State s = state();
		long now = System.currentTimeMillis();
		long remaining = TimerData.remainingNow(s, now);
		boolean flash = (now / 500) % 2 == 0;

		// big countdown
		int color = s.finished() ? (flash ? ALARM : INK) : (s.running() ? RUNNING : INK);
		String time = TimeFormat.countdown(remaining);
		float scale = 3.0f;
		int w = (int) (textRenderer.getWidth(time) * scale);
		ctx.getMatrices().push();
		ctx.getMatrices().translate(left + (PANEL_W - w) / 2f, top + 26, 0);
		ctx.getMatrices().scale(scale, scale, 1f);
		ctx.drawText(textRenderer, time, 0, 0, color, false);
		ctx.getMatrices().pop();

		// progress bar
		int bx = left + 14;
		int by = top + 58;
		int bw = PANEL_W - 28;
		ctx.fill(bx - 1, by - 1, bx + bw + 1, by + 7, 0xFF6B4A2B);
		ctx.fill(bx, by, bx + bw, by + 6, GRID);
		if (s.duration() > 0) {
			double used = s.finished() ? 1.0 : 1.0 - (double) remaining / s.duration();
			int fillColor = s.finished() ? ALARM : (s.running() ? RUNNING : PAUSED);
			ctx.fill(bx, by, bx + (int) (bw * Math.max(0, Math.min(1, used))), by + 6, fillColor);
		}

		// status line
		String status;
		int statusColor = MUTED;
		if (s.finished()) {
			status = "Time's up!";
			statusColor = flash ? ALARM : INK;
		} else if (s.running()) {
			status = "Running";
			statusColor = RUNNING;
		} else if (s.duration() == 0) {
			status = "Set a time below";
		} else if (TimerData.isSetup(s)) {
			status = "Ready";
		} else {
			status = "Paused";
			statusColor = PAUSED;
		}
		ctx.drawText(textRenderer, Text.literal(status).formatted(Formatting.BOLD),
				left + PANEL_W / 2 - textRenderer.getWidth(status) / 2, top + 72, statusColor, false);

		drawSteppers(ctx, s);

		super.render(ctx, mouseX, mouseY, delta);
	}

	private void drawSteppers(DrawContext ctx, State s) {
		String[] labels = {"Hours", "Minutes", "Seconds"};
		long d = s.duration();
		long[] values = {d / HOUR, (d / MINUTE) % 60, (d / SECOND) % 60};

		for (int i = 0; i < 3; i++) {
			int cx = left + 14 + 39 + i * 77;
			ctx.drawText(textRenderer, labels[i], cx - textRenderer.getWidth(labels[i]) / 2, top + 90, MUTED, false);

			String v = String.format("%02d", values[i]);
			float scale = 2.0f;
			int w = (int) (textRenderer.getWidth(v) * scale);
			ctx.getMatrices().push();
			ctx.getMatrices().translate(cx - w / 2f, top + 126, 0);
			ctx.getMatrices().scale(scale, scale, 1f);
			ctx.drawText(textRenderer, v, 0, 0, INK, false);
			ctx.getMatrices().pop();
		}
	}
}
