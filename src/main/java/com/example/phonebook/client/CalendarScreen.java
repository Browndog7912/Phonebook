package com.example.phonebook.client;

import com.example.phonebook.CalendarData;
import com.example.phonebook.CalendarData.Event;
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

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * A month calendar. Click a day to select it; days with events show a dot. The selected day's events
 * are listed underneath, where you can add events or remove them with the X. Uses today's real-world date.
 */
public class CalendarScreen extends Screen {
	private static final int PANEL_W = 260;
	private static final int PANEL_H = 252;

	private static final int GRID_X = 14;
	private static final int GRID_Y = 50;
	private static final int CELL_W = 33;
	private static final int CELL_H = 17;
	private static final int GRID_W = CELL_W * 7;

	private static final int LIST_Y = 166;
	private static final int LIST_ROWS = 4;
	private static final int LIST_ROW_H = 13;
	private static final int LIST_W = GRID_W;
	private static final int DEL_W = 12;

	private static final int INK = 0xFF3B2410;
	private static final int MUTED = 0xFF7A6240;
	private static final int GRID = 0xFFB59B66;
	private static final int HEADER_BG = 0xFF6B4A2B;
	private static final int CELL_A = 0xFFF6EDD2;
	private static final int CELL_B = 0xFFEFE2BC;
	private static final int CELL_EMPTY = 0xFFD9C9A0;
	private static final int HOVER = 0xFFDCE9F5;
	private static final int SELECTED = 0xFFB7D3EE;
	private static final int TODAY = 0xFF2F5FA8;
	private static final int DOT = 0xFFB03A2E;
	private static final int DEL_RED = 0xFFB03A2E;

	private static final DateTimeFormatter LONG_DATE = DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy", Locale.ENGLISH);

	private final ItemStack stack;
	private final Hand hand;
	private final List<Event> events;

	private YearMonth shown;
	private LocalDate selected;
	private int scroll;

	private int left;
	private int top;
	private TextFieldWidget field;

	public CalendarScreen(ItemStack stack, Hand hand) {
		super(Text.translatable("item.phonebook.calendar"));
		this.stack = stack;
		this.hand = hand;
		this.events = new ArrayList<>(CalendarData.read(stack));
		this.selected = LocalDate.now();
		this.shown = YearMonth.from(selected);
	}

	@Override
	protected void init() {
		left = (width - PANEL_W) / 2;
		top = (height - PANEL_H) / 2;

		String old = field == null ? "" : field.getText();

		addDrawableChild(ButtonWidget.builder(Text.literal("<"), b -> changeMonth(-1))
				.dimensions(left + 14, top + 8, 24, 20).build());
		addDrawableChild(ButtonWidget.builder(Text.literal(">"), b -> changeMonth(1))
				.dimensions(left + PANEL_W - 14 - 24, top + 8, 24, 20).build());
		addDrawableChild(ButtonWidget.builder(Text.literal("Today"), b -> goToday())
				.dimensions(left + PANEL_W - 14 - 24 - 4 - 44, top + 8, 44, 20).build());

		int fy = top + LIST_Y + LIST_ROWS * LIST_ROW_H + 6;
		field = new TextFieldWidget(textRenderer, left + 14, fy, LIST_W - 44, 20, Text.literal("New event"));
		field.setMaxLength(CalendarData.MAX_TEXT);
		field.setSuggestion("New event");
		field.setChangedListener(t -> field.setSuggestion(t.isEmpty() ? "New event" : null));
		field.setText(old);
		addDrawableChild(field);

		addDrawableChild(ButtonWidget.builder(Text.literal("Add"), b -> addEvent())
				.dimensions(left + 14 + LIST_W - 40, fy, 40, 20).build());

		setInitialFocus(field);
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

	/** Skip the vanilla dark gradient/blur so the world stays fully visible behind the calendar. */
	@Override
	public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
	}

	// ------------------------------------------------------------------ state

	private void changeMonth(int delta) {
		shown = shown.plusMonths(delta);
	}

	private void goToday() {
		selected = LocalDate.now();
		shown = YearMonth.from(selected);
		scroll = 0;
	}

	/** Indexes (into the master list) of the selected day's events, in order. */
	private List<Integer> dayIndices() {
		int key = CalendarData.key(selected);
		List<Integer> out = new ArrayList<>();
		for (int i = 0; i < events.size(); i++) {
			if (events.get(i).date() == key) {
				out.add(i);
			}
		}
		return out;
	}

	private int maxScroll() {
		return Math.max(0, dayIndices().size() - LIST_ROWS);
	}

	private void addEvent() {
		String text = field.getText().trim();
		if (text.isEmpty() || events.size() >= CalendarData.MAX_EVENTS) {
			return;
		}
		events.add(new Event(CalendarData.key(selected), text));
		field.setText("");
		commit();
		scroll = maxScroll(); // show the new event
	}

	private void removeEvent(int masterIndex) {
		events.remove(masterIndex);
		commit();
		scroll = Math.min(scroll, maxScroll());
	}

	/** Re-sorts by date, updates the local stack and syncs the list to the server. */
	private void commit() {
		events.sort(Comparator.comparingInt(Event::date)); // stable, so same-day order is kept

		CalendarData.write(stack, events); // instant local preview; the server copy syncs back
		PacketByteBuf buf = PacketByteBufs.create();
		buf.writeEnumConstant(hand);
		buf.writeVarInt(events.size());
		for (Event e : events) {
			buf.writeInt(e.date());
			buf.writeString(e.text());
		}
		ClientPlayNetworking.send(PhoneBookNetworking.SAVE_CALENDAR, buf);
	}

	// ------------------------------------------------------------------ input

	private int gridLeft() {
		return left + GRID_X;
	}

	private int gridTop() {
		return top + GRID_Y;
	}

	private int firstColumn() {
		return shown.atDay(1).getDayOfWeek().getValue() % 7; // Sunday = 0
	}

	@Override
	public boolean mouseClicked(double mx, double my, int button) {
		if (super.mouseClicked(mx, my, button)) {
			return true;
		}
		if (button != 0) {
			return false;
		}

		// day grid
		int gx = gridLeft();
		int gy = gridTop();
		if (mx >= gx && mx < gx + GRID_W && my >= gy && my < gy + CELL_H * 6) {
			int col = (int) ((mx - gx) / CELL_W);
			int row = (int) ((my - gy) / CELL_H);
			int day = row * 7 + col - firstColumn() + 1;
			if (day >= 1 && day <= shown.lengthOfMonth()) {
				selected = shown.atDay(day);
				scroll = 0;
			}
			return true;
		}

		// event list: the X deletes
		int lx = left + 14;
		int ly = top + LIST_Y;
		if (mx >= lx + LIST_W - DEL_W && mx < lx + LIST_W && my >= ly && my < ly + LIST_ROWS * LIST_ROW_H) {
			int r = (int) ((my - ly) / LIST_ROW_H);
			List<Integer> idx = dayIndices();
			int pos = scroll + r;
			if (pos < idx.size()) {
				removeEvent(idx.get(pos));
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean mouseScrolled(double mx, double my, double horizontal, double vertical) {
		if (vertical == 0) {
			return super.mouseScrolled(mx, my, horizontal, vertical);
		}
		int ly = top + LIST_Y;
		boolean overList = my >= ly && my < ly + LIST_ROWS * LIST_ROW_H;
		if (overList) {
			scroll = Math.max(0, Math.min(maxScroll(), scroll + (vertical < 0 ? 1 : -1)));
		} else {
			changeMonth(vertical < 0 ? 1 : -1);
		}
		return true;
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if ((keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) && field.isFocused()) {
			addEvent();
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

		// month title, centred between the "<" button and the "Today" button
		String title = shown.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + shown.getYear();
		int regionL = left + 14 + 24;
		int regionR = left + PANEL_W - 14 - 24 - 4 - 44;
		ctx.drawText(textRenderer, Text.literal(title).formatted(Formatting.BOLD),
				(regionL + regionR) / 2 - textRenderer.getWidth(title) / 2, top + 14, INK, false);

		drawWeekdays(ctx);
		drawGrid(ctx, mouseX, mouseY);
		drawDayEvents(ctx, mouseX, mouseY);

		super.render(ctx, mouseX, mouseY, delta);
	}

	private void drawWeekdays(DrawContext ctx) {
		String[] names = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
		int gx = gridLeft();
		int y = top + GRID_Y - 12;
		for (int c = 0; c < 7; c++) {
			String n = names[c];
			ctx.drawText(textRenderer, n, gx + c * CELL_W + (CELL_W - textRenderer.getWidth(n)) / 2, y, MUTED, false);
		}
	}

	private void drawGrid(DrawContext ctx, int mouseX, int mouseY) {
		int gx = gridLeft();
		int gy = gridTop();
		LocalDate today = LocalDate.now();

		Set<Integer> withEvents = new HashSet<>();
		for (Event e : events) {
			withEvents.add(e.date());
		}

		ctx.fill(gx - 1, gy - 1, gx + GRID_W + 1, gy + CELL_H * 6 + 1, HEADER_BG);

		int offset = firstColumn();
		int len = shown.lengthOfMonth();

		for (int cell = 0; cell < 42; cell++) {
			int col = cell % 7;
			int row = cell / 7;
			int cx = gx + col * CELL_W;
			int cy = gy + row * CELL_H;
			int day = cell - offset + 1;

			if (day < 1 || day > len) {
				ctx.fill(cx, cy, cx + CELL_W - 1, cy + CELL_H - 1, CELL_EMPTY);
				continue;
			}

			LocalDate date = shown.atDay(day);
			boolean isSelected = date.equals(selected);
			boolean hover = mouseX >= cx && mouseX < cx + CELL_W - 1 && mouseY >= cy && mouseY < cy + CELL_H - 1;

			int bg = (cell % 2 == 0) ? CELL_A : CELL_B;
			if (hover) {
				bg = HOVER;
			}
			if (isSelected) {
				bg = SELECTED;
			}
			ctx.fill(cx, cy, cx + CELL_W - 1, cy + CELL_H - 1, bg);

			if (date.equals(today)) { // blue outline marks today
				ctx.fill(cx, cy, cx + CELL_W - 1, cy + 1, TODAY);
				ctx.fill(cx, cy + CELL_H - 2, cx + CELL_W - 1, cy + CELL_H - 1, TODAY);
				ctx.fill(cx, cy, cx + 1, cy + CELL_H - 1, TODAY);
				ctx.fill(cx + CELL_W - 2, cy, cx + CELL_W - 1, cy + CELL_H - 1, TODAY);
			}

			ctx.drawText(textRenderer, String.valueOf(day), cx + 4, cy + 4, INK, false);

			if (withEvents.contains(CalendarData.key(date))) {
				ctx.fill(cx + CELL_W - 9, cy + 4, cx + CELL_W - 5, cy + 8, DOT);
			}
		}
	}

	private void drawDayEvents(DrawContext ctx, int mouseX, int mouseY) {
		int lx = left + 14;
		int ly = top + LIST_Y;

		ctx.drawText(textRenderer, Text.literal(LONG_DATE.format(selected)).formatted(Formatting.BOLD),
				lx, top + GRID_Y + CELL_H * 6 + 5, INK, false);

		List<Integer> idx = dayIndices();
		scroll = Math.max(0, Math.min(scroll, maxScroll()));

		ctx.fill(lx - 1, ly - 1, lx + LIST_W + 1, ly + LIST_ROWS * LIST_ROW_H + 1, HEADER_BG);

		for (int r = 0; r < LIST_ROWS; r++) {
			int ry = ly + r * LIST_ROW_H;
			int pos = scroll + r;
			boolean has = pos < idx.size();
			boolean hover = has && mouseX >= lx && mouseX < lx + LIST_W && mouseY >= ry && mouseY < ry + LIST_ROW_H;

			ctx.fill(lx, ry, lx + LIST_W, ry + LIST_ROW_H - 1, hover ? HOVER : (r % 2 == 0 ? CELL_A : CELL_B));
			if (!has) {
				continue;
			}

			Event e = events.get(idx.get(pos));
			ctx.drawText(textRenderer, textRenderer.trimToWidth(e.text(), LIST_W - DEL_W - 10), lx + 4, ry + 3, INK, false);

			boolean overDel = hover && mouseX >= lx + LIST_W - DEL_W;
			ctx.drawText(textRenderer, "x", lx + LIST_W - DEL_W + (DEL_W - textRenderer.getWidth("x")) / 2 - 1, ry + 3,
					overDel ? DEL_RED : GRID, false);
		}

		if (idx.isEmpty()) {
			String none = "No events - add one below.";
			ctx.drawText(textRenderer, none, lx + LIST_W / 2 - textRenderer.getWidth(none) / 2, ly + 18, MUTED, false);
		} else if (idx.size() > LIST_ROWS) {
			String more = (scroll + 1) + "-" + Math.min(idx.size(), scroll + LIST_ROWS) + " of " + idx.size();
			ctx.drawText(textRenderer, more, lx + LIST_W - textRenderer.getWidth(more),
					top + GRID_Y + CELL_H * 6 + 5, MUTED, false);
		}
	}
}
