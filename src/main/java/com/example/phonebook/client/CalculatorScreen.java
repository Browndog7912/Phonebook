package com.example.phonebook.client;

import com.example.phonebook.CalculatorEngine;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

/**
 * A pocket calculator. Click the keypad or type: digits, . + - * / = and % work, Enter equals,
 * Backspace deletes a digit and Delete clears. Whole-number results of 64 or more also show how many
 * item stacks that is. The calculator is not saved, so each use starts fresh.
 */
public class CalculatorScreen extends Screen {
	private static final int PANEL_W = 180;
	private static final int PANEL_H = 232;

	private static final int KEY_W = 38;
	private static final int KEY_H = 22;
	private static final int KEY_GAP = 4;
	private static final int KEY_X = 8;
	private static final int KEY_Y = 70;

	private static final int LCD_BG = 0xFFC9D6B0;
	private static final int LCD_BORDER = 0xFF3B2410;
	private static final int LCD_INK = 0xFF1F2A14;
	private static final int LCD_MUTED = 0xFF5E6B4A;
	private static final int LCD_ERROR = 0xFF8C2A1F;

	private final CalculatorEngine calc = new CalculatorEngine();

	private int left;
	private int top;

	public CalculatorScreen() {
		super(Text.translatable("item.phonebook.calculator"));
	}

	@Override
	protected void init() {
		left = (width - PANEL_W) / 2;
		top = (height - PANEL_H) / 2;

		key("√", 0, 0, calc::squareRoot);
		key("x²", 0, 1, calc::square);
		key("1/x", 0, 2, calc::reciprocal);
		key("Copy", 0, 3, () -> MinecraftClient.getInstance().keyboard.setClipboard(calc.copyText()));

		key("C", 1, 0, calc::clear);
		key("<-", 1, 1, calc::backspace);
		key("%", 1, 2, calc::percent);
		key("÷", 1, 3, () -> calc.operator('/'));

		key("7", 2, 0, () -> calc.digit(7));
		key("8", 2, 1, () -> calc.digit(8));
		key("9", 2, 2, () -> calc.digit(9));
		key("×", 2, 3, () -> calc.operator('*'));

		key("4", 3, 0, () -> calc.digit(4));
		key("5", 3, 1, () -> calc.digit(5));
		key("6", 3, 2, () -> calc.digit(6));
		key("-", 3, 3, () -> calc.operator('-'));

		key("1", 4, 0, () -> calc.digit(1));
		key("2", 4, 1, () -> calc.digit(2));
		key("3", 4, 2, () -> calc.digit(3));
		key("+", 4, 3, () -> calc.operator('+'));

		key("+/-", 5, 0, calc::negate);
		key("0", 5, 1, () -> calc.digit(0));
		key(".", 5, 2, calc::dot);
		key("=", 5, 3, calc::equals);
	}

	private void key(String label, int row, int col, Runnable action) {
		addDrawableChild(ButtonWidget.builder(Text.literal(label), b -> action.run())
				.dimensions(left + KEY_X + col * (KEY_W + KEY_GAP), top + KEY_Y + row * (KEY_H + KEY_GAP), KEY_W, KEY_H)
				.build());
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

	/** Skip the vanilla dark gradient/blur so the world stays fully visible behind the calculator. */
	@Override
	public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
	}

	// ------------------------------------------------------------------ keyboard

	@Override
	public boolean charTyped(char chr, int modifiers) {
		if (chr >= '0' && chr <= '9') {
			calc.digit(chr - '0');
			return true;
		}
		switch (chr) {
			case '.', ',' -> calc.dot();
			case '+', '-', '*', '/' -> calc.operator(chr);
			case 'x', 'X' -> calc.operator('*');
			case '=' -> calc.equals();
			case '%' -> calc.percent();
			default -> {
				return super.charTyped(chr, modifiers);
			}
		}
		return true;
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		switch (keyCode) {
			case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
				calc.equals();
				return true;
			}
			case GLFW.GLFW_KEY_BACKSPACE -> {
				calc.backspace();
				return true;
			}
			case GLFW.GLFW_KEY_DELETE -> {
				calc.clear();
				return true;
			}
			default -> {
				return super.keyPressed(keyCode, scanCode, modifiers);
			}
		}
	}

	// ------------------------------------------------------------------ rendering

	@Override
	public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
		// no renderBackground() call: that is what darkens the world behind the screen

		ctx.fill(left - 3, top - 3, left + PANEL_W + 3, top + PANEL_H + 3, 0xFF3B2410);
		ctx.fill(left, top, left + PANEL_W, top + PANEL_H, 0xFFEFE2BC);

		ctx.drawText(textRenderer, Text.literal("Calculator").formatted(Formatting.BOLD), left + 8, top + 4, 0xFF3B2410, false);

		drawDisplay(ctx);

		super.render(ctx, mouseX, mouseY, delta);
	}

	private void drawDisplay(DrawContext ctx) {
		int x0 = left + 8;
		int y0 = top + 16;
		int w = PANEL_W - 16;
		int h = 48;
		int right = x0 + w - 6;

		ctx.fill(x0 - 1, y0 - 1, x0 + w + 1, y0 + h + 1, LCD_BORDER);
		ctx.fill(x0, y0, x0 + w, y0 + h, LCD_BG);

		// small history line, e.g. "8 + 3 ="
		String history = textRenderer.trimToWidth(calc.history(), w - 12);
		ctx.drawText(textRenderer, history, right - textRenderer.getWidth(history), y0 + 4, LCD_MUTED, false);

		// main number, as large as fits
		String entry = calc.entry();
		int available = w - 12;
		int textW = textRenderer.getWidth(entry);
		float scale = 2.0f;
		if (textW * scale > available) {
			scale = 1.5f;
		}
		if (textW * scale > available) {
			scale = 1.0f;
		}
		int scaledW = (int) (textW * scale);
		ctx.getMatrices().push();
		ctx.getMatrices().translate(right - scaledW, y0 + 16, 0);
		ctx.getMatrices().scale(scale, scale, 1f);
		ctx.drawText(textRenderer, entry, 0, 0, calc.isError() ? LCD_ERROR : LCD_INK, false);
		ctx.getMatrices().pop();

		// stacks hint for big whole numbers
		String hint = calc.stacksHint();
		if (!hint.isEmpty()) {
			ctx.drawText(textRenderer, hint, right - textRenderer.getWidth(hint), y0 + h - 11, LCD_MUTED, false);
		}
	}
}
