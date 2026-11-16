package com.example.phonebook.client;

import com.example.phonebook.PhoneBookData;
import com.example.phonebook.PhoneBookData.Contact;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The phone book UI. Every page is a table (Name | Number). Contacts are grouped under
 * alphabet section header rows, and a clickable A-Z index on the right jumps to a section.
 */
public class PhoneBookScreen extends Screen {
	private static final int PANEL_W = 316;
	private static final int PANEL_H = 240;

	private static final int TABLE_X = 14;
	private static final int TABLE_Y = 32;
	private static final int NAME_COL = 160;
	private static final int NUM_COL = 90;
	private static final int TABLE_W = NAME_COL + NUM_COL;
	private static final int HEADER_H = 14;
	private static final int ROW_H = 12;
	private static final int ROWS = 10;

	private static final String LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ#";
	private static final int INDEX_X = TABLE_X + TABLE_W + 6;
	private static final int INDEX_CELL_W = 12;
	private static final int INDEX_CELL_H = 14;
	private static final int INDEX_PER_COL = 9;

	private static final int INK = 0xFF3B2410;
	private static final int GRID = 0xFFB59B66;
	private static final int HEADER_BG = 0xFF6B4A2B;
	private static final int SECTION_BG = 0xFFD9C48F;
	private static final int ROW_BG_A = 0xFFF6EDD2;
	private static final int ROW_BG_B = 0xFFEFE2BC;
	private static final int SELECTED_BG = 0xFFB7D3EE;

	/** One table row: a section header (contact == null) or a contact. */
	private record Row(char letter, Contact contact, boolean cont) {
	}

	private final ItemStack stack;
	private final Hand hand;
	private final List<Contact> contacts;
	private Contact selected;

	private List<List<Row>> pages = new ArrayList<>();
	private final Map<Character, Integer> letterPage = new HashMap<>();
	private int page;
	private int left;
	private int top;

	private TextFieldWidget nameField;
	private TextFieldWidget numberField;
	private ButtonWidget saveButton;
	private ButtonWidget deleteButton;
	private ButtonWidget prevButton;
	private ButtonWidget nextButton;

	public PhoneBookScreen(ItemStack stack, Hand hand) {
		super(Text.translatable("item.phonebook.phone_book"));
		this.stack = stack;
		this.hand = hand;
		this.contacts = new ArrayList<>(PhoneBookData.read(stack));
	}

	@Override
	protected void init() {
		left = (width - PANEL_W) / 2;
		top = (height - PANEL_H) / 2;

		String oldName = nameField == null ? "" : nameField.getText();
		String oldNumber = numberField == null ? "" : numberField.getText();

		int fy = top + 176;
		int x = left + 14;

		nameField = new TextFieldWidget(textRenderer, x, fy, 96, 20, Text.literal("Name"));
		nameField.setMaxLength(PhoneBookData.MAX_NAME);
		nameField.setSuggestion("Name");
		nameField.setChangedListener(t -> nameField.setSuggestion(t.isEmpty() ? "Name" : null));
		nameField.setText(oldName);
		addDrawableChild(nameField);
		x += 100;

		numberField = new TextFieldWidget(textRenderer, x, fy, 66, 20, Text.literal("Number"));
		numberField.setMaxLength(PhoneBookData.MAX_NUMBER);
		numberField.setSuggestion("Number");
		numberField.setChangedListener(t -> numberField.setSuggestion(t.isEmpty() ? "Number" : null));
		numberField.setText(oldNumber);
		addDrawableChild(numberField);
		x += 70;

		addDrawableChild(ButtonWidget.builder(Text.literal("Add"), b -> addContact())
				.dimensions(x, fy, 34, 20).build());
		x += 38;
		saveButton = addDrawableChild(ButtonWidget.builder(Text.literal("Save"), b -> saveSelected())
				.dimensions(x, fy, 40, 20).build());
		x += 44;
		deleteButton = addDrawableChild(ButtonWidget.builder(Text.literal("Del"), b -> deleteSelected())
				.dimensions(x, fy, 34, 20).build());

		prevButton = addDrawableChild(ButtonWidget.builder(Text.literal("<"), b -> changePage(-1))
				.dimensions(left + TABLE_X, top + 204, 50, 20).build());
		nextButton = addDrawableChild(ButtonWidget.builder(Text.literal(">"), b -> changePage(1))
				.dimensions(left + TABLE_X + TABLE_W - 50, top + 204, 50, 20).build());

		setInitialFocus(nameField);
		paginate();
		page = Math.min(page, pages.size() - 1);
		refreshButtons();
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

	/** Skip the vanilla dark gradient/blur so the world stays fully visible behind the book. */
	@Override
	public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
	}

	// ------------------------------------------------------------------ layout

	/** Builds the table pages. Each section starts with a header row; sections that run onto a new page repeat it as "(cont.)". */
	private void paginate() {
		pages = new ArrayList<>();
		letterPage.clear();
		List<Row> cur = new ArrayList<>();
		char lastSection = 0;

		for (Contact c : contacts) {
			char sec = PhoneBookData.sectionOf(c.name());
			if (sec != lastSection) {
				// a header needs room for at least one contact below it
				if (cur.size() + 2 > ROWS) {
					pages.add(cur);
					cur = new ArrayList<>();
				}
				letterPage.put(sec, pages.size());
				cur.add(new Row(sec, null, false));
				lastSection = sec;
			}
			if (cur.size() >= ROWS) {
				pages.add(cur);
				cur = new ArrayList<>();
				cur.add(new Row(sec, null, true));
			}
			cur.add(new Row(sec, c, false));
		}
		if (!cur.isEmpty() || pages.isEmpty()) {
			pages.add(cur);
		}
	}

	private void jumpTo(Contact target) {
		for (int p = 0; p < pages.size(); p++) {
			for (Row row : pages.get(p)) {
				if (row.contact() == target) {
					page = p;
					return;
				}
			}
		}
	}

	private void refreshButtons() {
		prevButton.active = page > 0;
		nextButton.active = page < pages.size() - 1;
		saveButton.active = selected != null;
		deleteButton.active = selected != null;
	}

	private void changePage(int delta) {
		page = Math.max(0, Math.min(pages.size() - 1, page + delta));
		refreshButtons();
	}

	// ------------------------------------------------------------------ editing

	private int indexOfIdentity(Contact c) {
		for (int i = 0; i < contacts.size(); i++) {
			if (contacts.get(i) == c) {
				return i;
			}
		}
		return -1;
	}

	private void clearFields() {
		nameField.setText("");
		numberField.setText("");
	}

	private void addContact() {
		String name = nameField.getText().trim();
		if (name.isEmpty() || contacts.size() >= PhoneBookData.MAX_CONTACTS) {
			return;
		}
		Contact c = new Contact(name, numberField.getText().trim());
		contacts.add(c);
		selected = null;
		clearFields();
		commit(c);
	}

	private void saveSelected() {
		String name = nameField.getText().trim();
		int i = selected == null ? -1 : indexOfIdentity(selected);
		if (i < 0 || name.isEmpty()) {
			return;
		}
		Contact c = new Contact(name, numberField.getText().trim());
		contacts.set(i, c);
		selected = null;
		clearFields();
		commit(c);
	}

	private void deleteSelected() {
		int i = selected == null ? -1 : indexOfIdentity(selected);
		if (i < 0) {
			return;
		}
		contacts.remove(i);
		selected = null;
		clearFields();
		commit(null);
	}

	/** Re-sorts into alphabet sections, rebuilds the table, and sends the list to the server. */
	private void commit(Contact focus) {
		contacts.sort(PhoneBookData.ORDER);
		paginate();
		if (focus != null) {
			jumpTo(focus);
		}
		page = Math.max(0, Math.min(pages.size() - 1, page));
		refreshButtons();

		PhoneBookData.write(stack, contacts); // instant local preview; the server copy syncs back
		PacketByteBuf buf = PacketByteBufs.create();
		buf.writeEnumConstant(hand);
		buf.writeVarInt(contacts.size());
		for (Contact c : contacts) {
			buf.writeString(c.name());
			buf.writeString(c.number());
		}
		ClientPlayNetworking.send(PhoneBookNetworking.SAVE, buf);
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

		// table rows
		int tx = left + TABLE_X;
		int ty = top + TABLE_Y + HEADER_H;
		if (mx >= tx && mx < tx + TABLE_W && my >= ty && my < ty + ROWS * ROW_H) {
			int r = (int) ((my - ty) / ROW_H);
			List<Row> rows = pages.get(page);
			if (r < rows.size() && rows.get(r).contact() != null) {
				Contact c = rows.get(r).contact();
				if (c == selected) {
					selected = null;
					clearFields();
				} else {
					selected = c;
					nameField.setText(c.name());
					numberField.setText(c.number());
				}
				refreshButtons();
				return true;
			}
		}

		// A-Z index
		for (int i = 0; i < LETTERS.length(); i++) {
			char ch = LETTERS.charAt(i);
			int cx = left + INDEX_X + (i / INDEX_PER_COL) * INDEX_CELL_W;
			int cy = top + TABLE_Y + (i % INDEX_PER_COL) * INDEX_CELL_H;
			if (mx >= cx && mx < cx + INDEX_CELL_W && my >= cy && my < cy + INDEX_CELL_H) {
				Integer target = letterPage.get(ch);
				if (target != null) {
					page = target;
					refreshButtons();
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
		if ((keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)
				&& (nameField.isFocused() || numberField.isFocused())) {
			if (selected != null) {
				saveSelected();
			} else {
				addContact();
			}
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	// ------------------------------------------------------------------ rendering

	@Override
	public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
		// no renderBackground() call: that is what darkens the world behind the screen

		// book cover + parchment page
		ctx.fill(left - 3, top - 3, left + PANEL_W + 3, top + PANEL_H + 3, 0xFF3B2410);
		ctx.fill(left, top, left + PANEL_W, top + PANEL_H, 0xFFEFE2BC);

		ctx.drawText(textRenderer, Text.literal("Phone Book").formatted(Formatting.BOLD), left + 14, top + 10, INK, false);
		String count = contacts.size() + (contacts.size() == 1 ? " contact" : " contacts");
		ctx.drawText(textRenderer, count, left + 14, top + 21, 0xFF7A6240, false);

		List<Row> rows = pages.get(page);
		drawSectionRange(ctx, rows);
		drawTable(ctx, rows);
		drawIndex(ctx, mouseX, mouseY);

		String pageLabel = "Page " + (page + 1) + " / " + pages.size();
		ctx.drawText(textRenderer, pageLabel,
				left + TABLE_X + TABLE_W / 2 - textRenderer.getWidth(pageLabel) / 2, top + 210, INK, false);

		String hint = "Click a row to edit it  |  Enter adds or saves  |  Scroll to flip pages";
		ctx.drawText(textRenderer, hint, left + PANEL_W / 2 - textRenderer.getWidth(hint) / 2, top + 228, 0xFF7A6240, false);

		super.render(ctx, mouseX, mouseY, delta);
	}

	private void drawSectionRange(DrawContext ctx, List<Row> rows) {
		if (rows.isEmpty()) {
			return;
		}
		char a = rows.get(0).letter();
		char b = rows.get(rows.size() - 1).letter();
		String label = a == b ? String.valueOf(a) : a + " - " + b;
		ctx.drawText(textRenderer, Text.literal(label).formatted(Formatting.BOLD),
				left + TABLE_X + TABLE_W - textRenderer.getWidth(label) - 2, top + 20, INK, false);
	}

	private void drawTable(DrawContext ctx, List<Row> rows) {
		int tx = left + TABLE_X;
		int ty = top + TABLE_Y;
		int bodyH = HEADER_H + ROWS * ROW_H;

		// outer border + grid colour underneath every cell
		ctx.fill(tx - 1, ty - 1, tx + TABLE_W + 1, ty + bodyH + 1, HEADER_BG);
		ctx.fill(tx, ty, tx + TABLE_W, ty + bodyH, GRID);

		// column header row
		ctx.fill(tx, ty, tx + NAME_COL - 1, ty + HEADER_H - 1, HEADER_BG);
		ctx.fill(tx + NAME_COL, ty, tx + TABLE_W, ty + HEADER_H - 1, HEADER_BG);
		ctx.drawText(textRenderer, "Name", tx + 4, ty + 3, 0xFFF4E8C8, false);
		ctx.drawText(textRenderer, "Number", tx + NAME_COL + 4, ty + 3, 0xFFF4E8C8, false);

		for (int r = 0; r < ROWS; r++) {
			int ry = ty + HEADER_H + r * ROW_H;
			int bottom = ry + ROW_H - 1;
			Row row = r < rows.size() ? rows.get(r) : null;

			if (row != null && row.contact() == null) {
				// alphabet section header spans both columns
				ctx.fill(tx, ry, tx + TABLE_W, bottom, SECTION_BG);
				String label = row.letter() + (row.cont() ? " (cont.)" : "");
				ctx.drawText(textRenderer, Text.literal(label).formatted(Formatting.BOLD), tx + 4, ry + 2, INK, false);
				continue;
			}

			int bg = (r % 2 == 0) ? ROW_BG_A : ROW_BG_B;
			if (row != null && row.contact() == selected) {
				bg = SELECTED_BG;
			}
			ctx.fill(tx, ry, tx + NAME_COL - 1, bottom, bg);
			ctx.fill(tx + NAME_COL, ry, tx + TABLE_W, bottom, bg);

			if (row != null) {
				Contact c = row.contact();
				ctx.drawText(textRenderer, textRenderer.trimToWidth(c.name(), NAME_COL - 8), tx + 4, ry + 2, INK, false);
				ctx.drawText(textRenderer, textRenderer.trimToWidth(c.number(), NUM_COL - 8), tx + NAME_COL + 4, ry + 2, INK, false);
			}
		}

		if (contacts.isEmpty()) {
			String empty = "No contacts yet - add one below.";
			ctx.drawText(textRenderer, empty, tx + TABLE_W / 2 - textRenderer.getWidth(empty) / 2,
					ty + HEADER_H + 4 * ROW_H, 0xFF7A6240, false);
		}
	}

	private void drawIndex(DrawContext ctx, int mouseX, int mouseY) {
		List<Row> rows = pages.get(page);
		for (int i = 0; i < LETTERS.length(); i++) {
			char ch = LETTERS.charAt(i);
			int cx = left + INDEX_X + (i / INDEX_PER_COL) * INDEX_CELL_W;
			int cy = top + TABLE_Y + (i % INDEX_PER_COL) * INDEX_CELL_H;
			boolean has = letterPage.containsKey(ch);
			boolean onThisPage = rows.stream().anyMatch(r -> r.letter() == ch);
			boolean hover = mouseX >= cx && mouseX < cx + INDEX_CELL_W && mouseY >= cy && mouseY < cy + INDEX_CELL_H;

			if (has && onThisPage) {
				ctx.fill(cx, cy, cx + INDEX_CELL_W - 1, cy + INDEX_CELL_H - 1, SECTION_BG);
			}
			if (has && hover) {
				ctx.fill(cx, cy, cx + INDEX_CELL_W - 1, cy + INDEX_CELL_H - 1, SELECTED_BG);
			}
			String s = String.valueOf(ch);
			ctx.drawText(textRenderer, s, cx + (INDEX_CELL_W - textRenderer.getWidth(s)) / 2 - 1, cy + 3,
					has ? INK : GRID, false);
		}
	}
}
