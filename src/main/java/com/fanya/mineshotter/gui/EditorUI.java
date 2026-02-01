package com.fanya.mineshotter.gui;

import com.fanya.mineshotter.logic.SelectionManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public class EditorUI {
    public boolean colorPickerOpen = false;
    public int currentColorIndex = 0;

    private final int[] minecraftPalette = {
            0xFF000000, 0xFF0000AA, 0xFF00AA00, 0xFF00AAAA, 0xFFAA0000, 0xFFAA00AA, 0xFFAA5500, 0xFFAAAAAA,
            0xFF555555, 0xFF5555FF, 0xFF55FF55, 0xFF55FFFF, 0xFFFF5555, 0xFFFF55FF, 0xFFFFFF55, 0xFFFFFFFF,
            0xFF1D1D21, 0xFFB02E26, 0xFF5E7C16, 0xFF835432, 0xFF3C44AA, 0xFF8932B8, 0xFF169C9C, 0xFF9D9D97,
            0xFF474F52, 0xFFF38BAA, 0xFF80C71F, 0xFFFED83D, 0xFF3AB3DA, 0xFFC74EBD, 0xFFFED8B0, 0xFFECECEC
    };

    private static final int PANEL_W = 200;
    private static final int PANEL_H = 58;
    private static final int PAD = 8;
    private static final int COLOR_BOX_W = 28;
    private static final int COLOR_BOX_H = 16;
    private static final int BTN_H = 18;
    private static final int BTN_GAP = 4;

    public interface EditorUIActions {
        void copy();
        void upload();
        void back();
    }

    public void render(DrawContext ctx, int mouseX, int mouseY, SelectionManager selection, MinecraftClient client, TextRenderer textRenderer) {
        int x1 = Math.min(selection.selX1, selection.selX2);
        int y1 = Math.min(selection.selY1, selection.selY2);

        int px = x1 + 8;
        int py = y1 + 8;

        // Проверка границ экрана
        if (px + PANEL_W > client.getWindow().getScaledWidth()) {
            px = client.getWindow().getScaledWidth() - PANEL_W - 8;
        }
        if (py + PANEL_H > client.getWindow().getScaledHeight()) {
            py = y1 - PANEL_H - 8;
        }

        // Фон панели
        ctx.fill(px, py, px + PANEL_W, py + PANEL_H, 0xFF151515);
        ctx.drawBorder(px, py, PANEL_W, PANEL_H, 0xFFFFFFFF);

        // Текст режима
        Text mode = selection.moveMode ? Text.translatable("mineshotter.editor.mode.move") : Text.translatable("mineshotter.editor.mode.draw");
        ctx.drawTextWithShadow(textRenderer, mode, px + PAD, py + PAD, 0xFFFFFF);

        // Цветовой квадрат
        int colorBoxX = px + PANEL_W - PAD - COLOR_BOX_W;
        int colorBoxY = py + PAD;
        ctx.fill(colorBoxX, colorBoxY, colorBoxX + COLOR_BOX_W, colorBoxY + COLOR_BOX_H, getCurrentColor());
        ctx.drawBorder(colorBoxX - 1, colorBoxY - 1, COLOR_BOX_W + 2, COLOR_BOX_H + 2, 0xFFFFFFFF);

        // Кнопки внизу
        int btnRowY = py + PANEL_H - BTN_H - PAD + 3;
        int btnRowX = px + PAD;
        int btnRowW = PANEL_W - PAD * 2;
        int btnCount = 4;
        int btnW = (btnRowW - (btnCount - 1) * BTN_GAP) / btnCount;

        Text toggleBtnText = selection.moveMode ? Text.translatable("mineshotter.editor.button.draw") : Text.translatable("mineshotter.editor.button.move");

        drawButton(ctx, textRenderer, btnRowX + 0 * (btnW + BTN_GAP), btnRowY, btnW, BTN_H, Text.translatable("mineshotter.editor.button.copy"), false, mouseX, mouseY);
        drawButton(ctx, textRenderer, btnRowX + 1 * (btnW + BTN_GAP), btnRowY, btnW, BTN_H, Text.translatable("mineshotter.editor.button.upload"), false, mouseX, mouseY);
        drawButton(ctx, textRenderer, btnRowX + 2 * (btnW + BTN_GAP), btnRowY, btnW, BTN_H, toggleBtnText, !selection.moveMode, mouseX, mouseY);
        drawButton(ctx, textRenderer, btnRowX + 3 * (btnW + BTN_GAP), btnRowY, btnW, BTN_H, Text.translatable("mineshotter.editor.button.back"), false, mouseX, mouseY);

        if (colorPickerOpen) {
            renderColorPicker(ctx, textRenderer, px, py + PANEL_H + 2, mouseX, mouseY);
        }
    }

    private void drawButton(DrawContext ctx, TextRenderer textRenderer, int x, int y, int w, int h, Text text, boolean active, int mouseX, int mouseY) {
        boolean hovered = isInside(mouseX, mouseY, x, y, w, h);

        int bg = active ? 0xFF3A3A90 : (hovered ? 0xFF404040 : 0xFF202020);
        ctx.fill(x, y, x + w, y + h, bg);
        ctx.drawBorder(x, y, w, h, 0xFF888888);

        int tw = textRenderer.getWidth(text);
        int tx = x + (w - tw) / 2;
        int ty = y + (h - textRenderer.fontHeight) / 2 + 1;
        ctx.drawTextWithShadow(textRenderer, text, tx, ty, 0xFFFFFF);
    }

    private void renderColorPicker(DrawContext ctx, TextRenderer textRenderer, int x, int y, int mouseX, int mouseY) {
        int cols = 8;
        int cell = 14;
        int pad = 6;
        int rows = (int) Math.ceil(Math.min(minecraftPalette.length, 64) / (double) cols);
        int w = pad * 2 + cols * cell;
        int h = pad * 2 + 12 + rows * cell;

        ctx.fill(x, y, x + w, y + h, 0xFF101010);
        ctx.drawBorder(x, y, w, h, 0xFFFFFFFF);

        ctx.drawTextWithShadow(textRenderer, Text.translatable("mineshotter.editor.palette"), x + pad, y + pad - 1, 0xFFFFFF);

        int startY = y + pad + 10;
        int count = Math.min(minecraftPalette.length, cols * rows);

        for (int i = 0; i < count; i++) {
            int r = i / cols;
            int c = i % cols;
            int px = x + pad + c * cell;
            int py = startY + r * cell;

            boolean hovered = isInside(mouseX, mouseY, px, py, cell, cell);
            boolean selected = i == currentColorIndex;

            ctx.fill(px, py, px + cell, py + cell, minecraftPalette[i]);

            if (hovered || selected) {
                int borderColor = selected ? 0xFFFFFF00 : 0xFFFFFFFF;
                ctx.drawBorder(px - 1, py - 1, cell + 2, cell + 2, borderColor);
            } else {
                ctx.drawBorder(px, py, cell, cell, 0xFF303030);
            }
        }
    }

    public boolean handleClick(int mx, int my, SelectionManager selection, MinecraftClient client, EditorUIActions actions) {
        int x1 = Math.min(selection.selX1, selection.selX2);
        int y1 = Math.min(selection.selY1, selection.selY2);

        int px = x1 + 8;
        int py = y1 + 8;
        if (px + PANEL_W > client.getWindow().getScaledWidth()) {
            px = client.getWindow().getScaledWidth() - PANEL_W - 8;
        }
        if (py + PANEL_H > client.getWindow().getScaledHeight()) {
            py = y1 - PANEL_H - 8;
        }

        int colorBoxX = px + PANEL_W - PAD - COLOR_BOX_W;
        int colorBoxY = py + PAD;
        if (isInside(mx, my, colorBoxX, colorBoxY, COLOR_BOX_W, COLOR_BOX_H)) {
            colorPickerOpen = !colorPickerOpen;
            return true;
        }

        int btnRowY = py + PANEL_H - BTN_H - PAD;
        int btnRowX = px + PAD;
        int btnRowW = PANEL_W - PAD * 2;
        int btnCount = 4;
        int btnW = (btnRowW - (btnCount - 1) * BTN_GAP) / btnCount;

        if (isInside(mx, my, btnRowX + 0 * (btnW + BTN_GAP), btnRowY, btnW, BTN_H)) {
            actions.copy();
            return true;
        }

        if (isInside(mx, my, btnRowX + 1 * (btnW + BTN_GAP), btnRowY, btnW, BTN_H)) {
            actions.upload();
            return true;
        }

        if (isInside(mx, my, btnRowX + 2 * (btnW + BTN_GAP), btnRowY, btnW, BTN_H)) {
            selection.moveMode = !selection.moveMode;
            colorPickerOpen = false;
            return true;
        }

        if (isInside(mx, my, btnRowX + 3 * (btnW + BTN_GAP), btnRowY, btnW, BTN_H)) {
            actions.back();
            return true;
        }

        if (colorPickerOpen) {
            if (handleColorPickerClick(mx, my, px, py + PANEL_H + 2)) {
                return true;
            }
        }

        return false;
    }

    private boolean handleColorPickerClick(int mx, int my, int x, int y) {
        int cols = 8;
        int cell = 14;
        int pad = 6;
        int rows = (int) Math.ceil(Math.min(minecraftPalette.length, 64) / (double) cols);
        int startY = y + pad + 10;
        int count = Math.min(minecraftPalette.length, cols * rows);

        for (int i = 0; i < count; i++) {
            int r = i / cols;
            int c = i % cols;
            int px = x + pad + c * cell;
            int py = startY + r * cell;
            if (isInside(mx, my, px, py, cell, cell)) {
                currentColorIndex = i;
                colorPickerOpen = false;
                return true;
            }
        }
        return false;
    }

    private boolean isInside(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    public int getCurrentColor() {
        return minecraftPalette[Math.min(currentColorIndex, minecraftPalette.length - 1)];
    }
}