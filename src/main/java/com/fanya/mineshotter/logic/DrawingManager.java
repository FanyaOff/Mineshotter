package com.fanya.mineshotter.logic;

import com.fanya.mineshotter.model.LineSegment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public class DrawingManager {
    public final List<LineSegment> lines = new ArrayList<>();
    public boolean isDrawing = false;
    public int lastDrawX, lastDrawY;

    private NativeImageBackedTexture linesTexture;
    private Identifier linesTextureId;
    private NativeImage linesImage;
    public boolean linesDirty = true;

    public static final int LINE_THICKNESS = 2;

    public void init(MinecraftClient client) {
        int sw = client.getWindow().getScaledWidth();
        int sh = client.getWindow().getScaledHeight();
        linesImage = new NativeImage(sw, sh, true);
        linesTexture = new NativeImageBackedTexture(linesImage);
        linesTextureId = client.getTextureManager().registerDynamicTexture("mineshotter_lines", linesTexture);
    }

    public void renderLines(DrawContext context, MinecraftClient client) {
        if (linesDirty) {
            clearLinesTexture();
            for (LineSegment l : lines) {
                drawThickLineOnImage(linesImage, l.x1, l.y1, l.x2, l.y2, l.color,
                        LINE_THICKNESS,
                        client.getWindow().getScaledWidth(),
                        client.getWindow().getScaledHeight());
            }
            linesTexture.upload();
            linesDirty = false;
        }

        int sw = client.getWindow().getScaledWidth();
        int sh = client.getWindow().getScaledHeight();
        context.drawTexture(linesTextureId, 0, 0, 0, 0, sw, sh, sw, sh);
    }

    private void clearLinesTexture() {
        int w = linesImage.getWidth();
        int h = linesImage.getHeight();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                linesImage.setColor(x, y, 0);
            }
        }
    }

    public static void drawThickLineOnImage(NativeImage img, int x0, int y0, int x1, int y1, int color, int thickness, int w, int h) {
        for (int i = -(thickness/2); i <= (thickness/2); i++) {
            for (int j = -(thickness/2); j <= (thickness/2); j++) {
                bresenhamLineOnImage(img, x0 + i, y0 + j, x1 + i, y1 + j, color, w, h);
            }
        }
    }

    private static void bresenhamLineOnImage(NativeImage img, int x0, int y0, int x1, int y1, int color, int w, int h) {
        int a = (color >>> 24) & 0xFF;
        int r = (color >>> 16) & 0xFF;
        int g = (color >>>  8) & 0xFF;
        int b = (color >>>  0) & 0xFF;
        int abgr = (a << 24) | (b << 16) | (g << 8) | r;

        int dx = Math.abs(x1 - x0), dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1, sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;

        while (true) {
            if (x0 >= 0 && x0 < w && y0 >= 0 && y0 < h) {
                img.setColor(x0, y0, abgr);
            }
            if (x0 == x1 && y0 == y1) break;
            int e2 = 2 * err;
            if (e2 > -dy) { err -= dy; x0 += sx; }
            if (e2 < dx)  { err += dx; y0 += sy; }
        }
    }

    private void drawThickLine(DrawContext context, int x0, int y0, int x1, int y1, int color) {
        for (int i = -(LINE_THICKNESS/2); i <= (LINE_THICKNESS/2); i++) {
            for (int j = -(LINE_THICKNESS/2); j <= (LINE_THICKNESS/2); j++) {
                bresenhamLine(context, x0 + i, y0 + j, x1 + i, y1 + j, color);
            }
        }
    }

    private void bresenhamLine(DrawContext context, int x0, int y0, int x1, int y1, int color) {
        int dx = Math.abs(x1 - x0), dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1, sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;

        while (true) {
            context.fill(x0, y0, x0 + 1, y0 + 1, color);
            if (x0 == x1 && y0 == y1) break;
            int e2 = 2 * err;
            if (e2 > -dy) { err -= dy; x0 += sx; }
            if (e2 < dx)  { err += dx; y0 += sy; }
        }
    }

    public void close(MinecraftClient client) {
        if (linesTexture != null) {
            client.getTextureManager().destroyTexture(linesTextureId);
            linesImage.close();
        }
    }
}
