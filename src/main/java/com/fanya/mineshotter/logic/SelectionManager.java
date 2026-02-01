package com.fanya.mineshotter.logic;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public class SelectionManager {
    public int selX1 = -1, selY1 = -1, selX2 = -1, selY2 = -1;
    public boolean isSelecting = false;
    public boolean selectionConfirmed = false;
    public boolean moveMode = true;
    public boolean isDragging = false;
    public int dragType = -1;
    public int dragOffsetX, dragOffsetY;

    public static final int MIN_SEL_SIZE = 20;
    private static final int HAND_HIT = 10;

    public void renderDimming(DrawContext context, int sw, int sh, TextRenderer textRenderer) {
        if (selX1 == -1) {
            context.fill(0, 0, sw, sh, 0xCC000000);
            int cx = sw / 2;
            int cy = sh / 2;
            context.fill(cx - 100, cy - 20, cx + 100, cy + 20, 0xFF000000);
            context.drawBorder(cx - 100, cy - 20, 200, 40, 0xFFFFFFFF);
            context.drawCenteredTextWithShadow(textRenderer, Text.translatable("mineshotter.editor.tip1"), cx, cy - 10, 0xFFFFFFFF);
            context.drawCenteredTextWithShadow(textRenderer, Text.translatable("mineshotter.editor.tip2"), cx, cy + 2, 0xFFAAAAAA);
            return;
        }

        int x1 = Math.min(selX1, selX2);
        int y1 = Math.min(selY1, selY2);
        int x2 = Math.max(selX1, selX2);
        int y2 = Math.max(selY1, selY2);

        int dim = 0xCC000000;
        context.fill(0, 0, sw, y1, dim);
        context.fill(0, y2, sw, sh, dim);
        context.fill(0, y1, x1, y2, dim);
        context.fill(x2, y1, sw, y2, dim);

        context.drawBorder(x1 - 1, y1 - 1, (x2 - x1) + 2, (y2 - y1) + 2, 0xFF000000);
        context.drawBorder(x1, y1, x2 - x1, y2 - y1, 0xFFFFFFFF);

        String size = (x2 - x1) + "x" + (y2 - y1);
        int tw = textRenderer.getWidth(size);
        context.fill(x1, y1 - 14, x1 + tw + 4, y1, 0xFF000000);
        context.drawTextWithShadow(textRenderer, size, x1 + 2, y1 - 12, 0xFFFFFFFF);

        if (selectionConfirmed && moveMode) {
            renderResizeHandles(context, x1, y1, x2, y2);
        }
    }

    private void renderResizeHandles(DrawContext ctx, int x1, int y1, int x2, int y2) {
        int c = 0xFFFFFFFF;
        fillHandle(ctx, x1, y1, c);
        fillHandle(ctx, x2, y1, c);
        fillHandle(ctx, x1, y2, c);
        fillHandle(ctx, x2, y2, c);
        fillHandle(ctx, (x1 + x2) / 2, y1, c);
        fillHandle(ctx, x2, (y1 + y2) / 2, c);
        fillHandle(ctx, (x1 + x2) / 2, y2, c);
        fillHandle(ctx, x1, (y1 + y2) / 2, c);
    }

    private void fillHandle(DrawContext ctx, int cx, int cy, int color) {
        int r = 4;
        ctx.fill(cx - r, cy - r, cx + r + 1, cy + r + 1, 0xFF000000);
        ctx.drawBorder(cx - r, cy - r, 2 * r + 1, 2 * r + 1, color);
    }

    public boolean beginMoveResize(int mx, int my) {
        int x1 = Math.min(selX1, selX2); int y1 = Math.min(selY1, selY2);
        int x2 = Math.max(selX1, selX2); int y2 = Math.max(selY1, selY2);
        int cx = (x1 + x2) / 2; int cy = (y1 + y2) / 2;

        if (hit(mx, my, x1, y1)) dragType = 1;
        else if (hit(mx, my, x2, y1)) dragType = 2;
        else if (hit(mx, my, x1, y2)) dragType = 3;
        else if (hit(mx, my, x2, y2)) dragType = 4;
        else if (hit(mx, my, cx, y1)) dragType = 5;
        else if (hit(mx, my, x2, cy)) dragType = 6;
        else if (hit(mx, my, cx, y2)) dragType = 7;
        else if (hit(mx, my, x1, cy)) dragType = 8;
        else {
            if (!isInSelection(mx, my)) return false;
            dragType = 0; dragOffsetX = mx - x1; dragOffsetY = my - y1;
        }
        isDragging = true;
        return true;
    }

    private boolean hit(int mx, int my, int hx, int hy) {
        return mx >= hx - HAND_HIT && mx <= hx + HAND_HIT && my >= hy - HAND_HIT && my <= hy + HAND_HIT;
    }

    public void applyMoveResize(int mx, int my) {
        int x1 = Math.min(selX1, selX2); int y1 = Math.min(selY1, selY2);
        int w = Math.abs(selX2 - selX1); int h = Math.abs(selY2 - selY1);

        switch (dragType) {
            case 0 -> { int nx1 = mx - dragOffsetX; int ny1 = my - dragOffsetY; selX1 = nx1; selY1 = ny1; selX2 = nx1 + w; selY2 = ny1 + h; }
            case 1 -> { selX1 = mx; selY1 = my; }
            case 2 -> { selX2 = mx; selY1 = my; }
            case 3 -> { selX1 = mx; selY2 = my; }
            case 4 -> { selX2 = mx; selY2 = my; }
            case 5 -> selY1 = my;
            case 6 -> selX2 = mx;
            case 7 -> selY2 = my;
            case 8 -> selX1 = mx;
        }
        int nx1 = Math.min(selX1, selX2); int ny1 = Math.min(selY1, selY2);
        int nx2 = Math.max(selX1, selX2); int ny2 = Math.max(selY1, selY2);
        if (nx2 - nx1 < MIN_SEL_SIZE) nx2 = nx1 + MIN_SEL_SIZE;
        if (ny2 - ny1 < MIN_SEL_SIZE) ny2 = ny1 + MIN_SEL_SIZE;
        selX1 = nx1; selY1 = ny1; selX2 = nx2; selY2 = ny2;
    }

    public void confirmSelection() {
        if (Math.abs(selX2 - selX1) >= MIN_SEL_SIZE && Math.abs(selY2 - selY1) >= MIN_SEL_SIZE) selectionConfirmed = true;
        else resetSelection();
    }

    public void resetSelection() {
        selX1 = selX2 = selY1 = selY2 = -1;
        selectionConfirmed = false;
        isSelecting = false; isDragging = false;
        dragType = -1;
        moveMode = true;
    }

    public boolean isInSelection(int x, int y) {
        int x1 = Math.min(selX1, selX2); int x2 = Math.max(selX1, selX2);
        int y1 = Math.min(selY1, selY2); int y2 = Math.max(selY1, selY2);
        return x >= x1 && x <= x2 && y >= y1 && y <= y2;
    }
}
