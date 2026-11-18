package com.example.phonebook.client;

import com.example.phonebook.GameClock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Shows the in-game time (left) and the real-world time (right), each with an analog face and a
 * digital readout. A button switches between 12-hour and 24-hour display.
 */
public class ClockScreen extends Screen {
	private static final int PANEL_W = 300;
	private static final int PANEL_H = 218;

	private static final int FACE_R = 34;

	private static final int INK = 0xFF3B2410;
	private static final int MUTED = 0xFF7A6240;
	private static final int GRID = 0xFFB59B66;
	private static final int FACE_RIM = 0xFF6B4A2B;
	private static final int FACE_BG = 0xFFFFFAEB;
	private static final int HAND_RED = 0xFFB03A2E;

	private static final DateTimeFormatter LONG_DATE = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy", Locale.ENGLISH);

	/** Remembered while the game is running. */
	private static boolean use24h = true;

	private int left;
	private int top;
	private ButtonWidget formatButton;

	public ClockScreen() {
		super(Text.translatable("item.phonebook.clock"));
	}

	@Override
	protected void init() {
		left = (width - PANEL_W) / 2;
		top = (height - PANEL_H) / 2;

		formatButton = addDrawableChild(ButtonWidget.builder(Text.literal(formatLabel()), b -> {
			use24h = !use24h;
			b.setMessage(Text.literal(formatLabel()));
		}).dimensions(left + PANEL_W / 2 - 45, top + PANEL_H - 30, 90, 20).build());
	}

	private static String formatLabel() {
		return use24h ? "Format: 24h" : "Format: 12h";
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

	/** Skip the vanilla dark gradient/blur so the world stays fully visible behind the clock. */
	@Override
	public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
	}

	// ------------------------------------------------------------------ rendering

	@Override
	public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
		// no renderBackground() call: that is what darkens the world behind the screen

		ctx.fill(left - 3, top - 3, left + PANEL_W + 3, top + PANEL_H + 3, 0xFF3B2410);
		ctx.fill(left, top, left + PANEL_W, top + PANEL_H, 0xFFEFE2BC);
		ctx.fill(left + PANEL_W / 2, top + 10, left + PANEL_W / 2 + 1, top + PANEL_H - 40, GRID);

		int leftCx = left + 14 + 65 + 1;
		int rightCx = left + PANEL_W - 14 - 65 - 1;

		drawGame(ctx, leftCx);
		drawReal(ctx, rightCx);

		super.render(ctx, mouseX, mouseY, delta);
	}

	private void drawGame(DrawContext ctx, int cx) {
		centered(ctx, Text.literal("Game Time").formatted(Formatting.BOLD), cx, top + 10, INK);

		ClientWorld world = MinecraftClient.getInstance().world;
		if (world == null) {
			return;
		}
		long timeOfDay = world.getTimeOfDay();
		int tick = GameClock.tickOfDay(timeOfDay);
		boolean fixed = world.getDimension().hasFixedTime();

		double hours = GameClock.hoursFloat(tick);
		double minutes = (hours - Math.floor(hours)) * 60.0;
		drawFace(ctx, cx, top + 62, hours, minutes, Double.NaN);

		bigCentered(ctx, GameClock.format(tick, use24h), cx, top + 106, INK, use24h ? 2.5f : 2.0f);

		int y = top + 130;
		String phase = fixed ? "No day/night cycle" : GameClock.phase(tick);
		int phaseW = textRenderer.getWidth(phase);
		int swatch = fixed ? 0 : 8;
		int startX = cx - (phaseW + (fixed ? 0 : swatch + 4)) / 2;
		if (!fixed) {
			ctx.fill(startX, y, startX + swatch, y + swatch, 0xFF3B2410);
			ctx.fill(startX + 1, y + 1, startX + swatch - 1, y + swatch - 1, GameClock.phaseColor(tick));
			startX += swatch + 4;
		}
		ctx.drawText(textRenderer, phase, startX, y, INK, false);

		centered(ctx, Text.literal("Day " + GameClock.dayNumber(timeOfDay)), cx, top + 144, INK);
		centered(ctx, Text.literal("Tick " + tick + " / " + GameClock.DAY_TICKS), cx, top + 157, MUTED);
	}

	private void drawReal(DrawContext ctx, int cx) {
		centered(ctx, Text.literal("Real Time").formatted(Formatting.BOLD), cx, top + 10, INK);

		LocalTime now = LocalTime.now();
		double sec = now.getSecond() + now.getNano() / 1_000_000_000.0;
		double minutes = now.getMinute() + sec / 60.0;
		double hours = now.getHour() + minutes / 60.0;
		drawFace(ctx, cx, top + 62, hours, minutes, sec);

		String time = use24h
				? now.format(DateTimeFormatter.ofPattern("HH:mm:ss", Locale.ENGLISH))
				: now.format(DateTimeFormatter.ofPattern("h:mm:ss a", Locale.ENGLISH));
		bigCentered(ctx, time, cx, top + 106, INK, use24h ? 1.9f : 1.5f);

		centered(ctx, Text.literal(LocalDate.now().format(LONG_DATE)), cx, top + 130, INK);
		String zone = textRenderer.trimToWidth(ZoneId.systemDefault().getId(), 126);
		centered(ctx, Text.literal(zone), cx, top + 144, MUTED);
	}

	private void centered(DrawContext ctx, Text text, int cx, int y, int color) {
		ctx.drawText(textRenderer, text, cx - textRenderer.getWidth(text) / 2, y, color, false);
	}

	/** Draws text scaled up, horizontally centred on cx, with its top edge at y. */
	private void bigCentered(DrawContext ctx, String text, int cx, int y, int color, float scale) {
		int w = (int) (textRenderer.getWidth(text) * scale);
		ctx.getMatrices().push();
		ctx.getMatrices().translate(cx - w / 2f, y, 0);
		ctx.getMatrices().scale(scale, scale, 1f);
		ctx.drawText(textRenderer, text, 0, 0, color, false);
		ctx.getMatrices().pop();
	}

	// ------------------------------------------------------------------ analog face

	/**
	 * @param hours   0-24 (fractional) position of the hour hand
	 * @param minutes 0-60 (fractional) position of the minute hand
	 * @param seconds 0-60 (fractional) position of the second hand, or NaN for none
	 */
	private void drawFace(DrawContext ctx, int cx, int cy, double hours, double minutes, double seconds) {
		fillDisk(ctx, cx, cy, FACE_R, FACE_RIM);
		fillDisk(ctx, cx, cy, FACE_R - 2, FACE_BG);

		for (int i = 0; i < 12; i++) {
			double a = Math.toRadians(i * 30);
			int inner = (i % 3 == 0) ? FACE_R - 9 : FACE_R - 6;
			line(ctx, cx + Math.sin(a) * inner, cy - Math.cos(a) * inner,
					cx + Math.sin(a) * (FACE_R - 4), cy - Math.cos(a) * (FACE_R - 4), INK, 1);
		}

		hand(ctx, cx, cy, (hours % 12) * 30.0, FACE_R * 0.50, INK, 2);
		hand(ctx, cx, cy, minutes * 6.0, FACE_R * 0.74, INK, 1);
		if (!Double.isNaN(seconds)) {
			hand(ctx, cx, cy, seconds * 6.0, FACE_R * 0.80, HAND_RED, 1);
		}
		ctx.fill(cx - 1, cy - 1, cx + 2, cy + 2, INK);
	}

	private void hand(DrawContext ctx, int cx, int cy, double degrees, double length, int color, int thickness) {
		double a = Math.toRadians(degrees);
		line(ctx, cx, cy, cx + Math.sin(a) * length, cy - Math.cos(a) * length, color, thickness);
	}

	private void fillDisk(DrawContext ctx, int cx, int cy, int r, int color) {
		for (int dy = -r; dy <= r; dy++) {
			int dx = (int) Math.round(Math.sqrt((double) r * r - (double) dy * dy));
			ctx.fill(cx - dx, cy + dy, cx + dx + 1, cy + dy + 1, color);
		}
	}

	/** Draws a line by stepping along it and filling small squares (no line primitive in DrawContext). */
	private void line(DrawContext ctx, double x0, double y0, double x1, double y1, int color, int thickness) {
		int steps = (int) Math.ceil(Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0)));
		steps = Math.max(1, steps);
		for (int i = 0; i <= steps; i++) {
			double t = (double) i / steps;
			int x = (int) Math.round(x0 + (x1 - x0) * t);
			int y = (int) Math.round(y0 + (y1 - y0) * t);
			ctx.fill(x, y, x + thickness, y + thickness, color);
		}
	}
}
